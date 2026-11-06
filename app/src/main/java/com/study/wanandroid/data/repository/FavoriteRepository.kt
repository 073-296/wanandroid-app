package com.study.wanandroid.data.repository

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.study.wanandroid.data.local.dao.CategoryDao
import com.study.wanandroid.data.local.dao.CategoryWithCount
import com.study.wanandroid.data.local.dao.FavoriteDao
import com.study.wanandroid.data.local.entity.CategoryEntity
import com.study.wanandroid.data.local.entity.FavoriteArticleEntity
import com.study.wanandroid.data.local.entity.RemoteSyncState
import com.study.wanandroid.data.remote.ApiService
import com.study.wanandroid.data.remote.dto.FavoriteDetail
import com.study.wanandroid.data.sync.FavoriteSyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FavoriteRepository(
    private val context: Context,
    private val apiService: ApiService,
    private val favoriteDao: FavoriteDao,
    private val categoryDao: CategoryDao
) {
    //1.观察数据流（给UI用）
    fun observeFavoriteIds(userId: Long): Flow<Set<Long>> =
        favoriteDao.observeFavoriteIds(userId).map { it.toSet() }

    fun observeFavorites(userId: Long, categoryId: Long?): Flow<List<FavoriteArticleEntity>> {
        return if (categoryId == null) {
            favoriteDao.observeAllFavorites(userId)
        } else {
            favoriteDao.observeFavoritesByCategory(userId, categoryId)
        }
    }

    fun observeCategoriesWithCount(userId: Long): Flow<List<CategoryWithCount>> =
        categoryDao.observeCategoriesWithCount(userId)

    fun observeUncategorizedFavorites(userId: Long): Flow<List<FavoriteArticleEntity>> =
        favoriteDao.observeUncategorizedFavorites(userId)

    fun observeTotalFavoriteCount(userId: Long) =
        favoriteDao.observeTotalFavoriteCount(userId)

    fun observeUncategorizedCount(userId: Long) =
        favoriteDao.observeUncategorizedCount(userId)

    //2.切换收藏状态 (乐观 UI + 任务入队)
    suspend fun toggleFavorite(userId: Long, article: FavoriteArticleEntity) {
        val local = favoriteDao.getFavorite(userId, article.articleId)

        if (local == null || local.remoteSyncState == RemoteSyncState.PENDING_DELETE) {
            //情况一：本地没有，或者正在删除中（用户悔恨了，重新收藏）
            val entity = if (local != null) {
                //取消->收藏
                local.copy(remoteSyncState = RemoteSyncState.PENDING_ADD, lastError = null)
            } else {
                //全新收藏
                article.copy(
                    userId = userId,
                    collectTime = System.currentTimeMillis(),
                    serverOrder = -1,//本地新增排最前
                    remoteSyncState = RemoteSyncState.PENDING_ADD
                )
            }
            favoriteDao.upsert(entity)
            enqueueWorker(userId, entity.articleId, isAdd = true)
        } else if (local.remoteSyncState == RemoteSyncState.PENDING_ADD) {
            //情况二：刚收藏还没上传，又点取消(收藏->取消)
            favoriteDao.deleteById(userId, article.articleId)
            //取消之前的WorkManager任务，防止重复发请求
            WorkManager.Companion.getInstance(context).cancelUniqueWork("add_${userId}_${article.articleId}")
        } else if (local.remoteSyncState == RemoteSyncState.SYNCED) {
            //情况三：正常取消收藏
            favoriteDao.markAsPendingDelete(userId, article.articleId)
            enqueueWorker(userId, article.articleId, isAdd = false)
        }
    }

    //3.分类操作
    suspend fun addCategory(userId: Long, name: String): Result<Unit> = runCatching {
        val check = categoryDao.checkNameExists(userId, name)
        if (check > 0) throw Exception("分类名已存在")
        val maxOrder = categoryDao.getMaxSortOrder(userId) ?: 0
        categoryDao.insert(CategoryEntity(userId = userId, name = name, sortOrder = maxOrder + 1))
    }

    suspend fun deleteCategory(categoryId: Long) {
        categoryDao.deleteById(categoryId)// 外键 SET_NULL 自动处理文章关联
    }

    suspend fun setArticleCategory(userId: Long, articleId: Long, categoryId: Long?): Result<Unit> =
        runCatching {
            val targetCategoryId = if (categoryId == -1L) null else categoryId
            if (targetCategoryId != null) {
                val cat = categoryDao.getCategoryById(targetCategoryId, userId)
                if (cat == null) throw Exception("分类不存在或不属于当前用户")
            }
            favoriteDao.updateCategory(userId, articleId, targetCategoryId)
        }

    suspend fun renameCategory(userId: Long, categoryId: Long, newName: String): Result<Unit> =
        runCatching {
            val check = categoryDao.checkNameExists(userId, newName, categoryId)
            if (check > 0) throw Exception("分类名已存在")
            val cat =
                categoryDao.getCategoryById(categoryId, userId) ?: throw Exception("分类不存在")
            categoryDao.update(cat.copy(name = newName))
        }

    //4.全量对账与分页拉取
    /**
     * 整体刷新：拉取 collectIds 对账 + 分页拉取详情
     * 建议在进入收藏界面或下拉刷新时调用
     */
    suspend fun refreshAll(userId: Long) {
        //分页拉取收藏列表（服务端真实收藏数据），同时收集所有 originId 用于对账
        val remoteIds = mutableSetOf<Long>()
        var page = 0
        var indexOffset = 0
        while (true) {
            val resp = apiService.getFavoriteList(page)
            //登录失效等业务错误直接抛出，绝不能拿不完整的数据做对账
            if (resp.errorCode != 0) throw Exception(resp.errorMsg.ifEmpty { "刷新失败" })
            val data = resp.data ?: break
            if (data.datas.isEmpty()) break
            for (item in data.datas) {
                mergeRemoteData(userId, item, indexOffset++)
                remoteIds.add(item.originId)
            }
            if (data.over) break
            page++
        }
        //以列表真实数据为准对账：删掉本地 SYNCED 但服务端列表里已经没有的文章
        val staleIds=favoriteDao.getSyncedArticleIds(userId).filterNot { it in remoteIds }
        staleIds.forEach { favoriteDao.deleteById(userId,it) }

        //清理卡死的 PENDING_ADD：服务端列表里没有，说明上传早已失败
        favoriteDao.getPendingAdds(userId)
            .filterNot { it.articleId in remoteIds }
            .forEach { favoriteDao.deleteById(userId,it.articleId) }
    }

    //合并单条远程数据（防覆盖本地分类和状态）
    private suspend fun mergeRemoteData(userId: Long, remote: FavoriteDetail, serverOrder: Int) {
        val local = favoriteDao.getFavorite(userId, remote.originId)

        //如果本地正在等待上传或等待删除，直接跳过，绝不覆盖
        if (local?.remoteSyncState == RemoteSyncState.PENDING_ADD ||
            local?.remoteSyncState == RemoteSyncState.PENDING_DELETE
        ) {
            return
        } else {
            val entity = FavoriteArticleEntity(
                userId = userId,
                articleId = remote.originId,
                title = remote.title,
                author = if (remote.author.isNotBlank()) remote.author else local?.author,
                link = remote.link,
                niceDate = remote.niceDate,
                publishTime = remote.publishTime,
                serverOrder = serverOrder,
                //保留本地的分类、时间戳
                collectTime = local?.collectTime ?: 0L,
                categoryId = local?.categoryId,
                remoteSyncState = RemoteSyncState.SYNCED,
                lastError = null
            )
            favoriteDao.upsert(entity)
        }
    }

    //5.workManager任务入队
    private fun enqueueWorker(userId: Long, articleId: Long, isAdd: Boolean) {
        val data = Data.Builder()
            .putLong("userId", userId)
            .putLong("articleId", articleId)
            .putBoolean("isAdd", isAdd)
            .build()

        val request = OneTimeWorkRequestBuilder<FavoriteSyncWorker>() // 统一用一个 Worker 处理
            .setInputData(data)
            .addTag("user_$userId")
            .build()

        /**
         * 处理用户同一篇文章反复收藏、取消
         * 用唯一任务名（带上 userId 和 articleId），策略选 REPLACE。
         * 如果队列里已经有一个要发同一篇文章的任务了，新任务会直接替换掉旧任务。保证了哪怕点一万次，最终只发一次符合最新状态的请求。
         * */
        val uniqueWorkName =
            if (isAdd) "add_${userId}_${articleId}" else "delete_${userId}_${articleId}"

        WorkManager.Companion.getInstance(context).enqueueUniqueWork(
            uniqueWorkName,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }
}