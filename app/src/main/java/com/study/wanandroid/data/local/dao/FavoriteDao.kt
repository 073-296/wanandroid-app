package com.study.wanandroid.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.study.wanandroid.data.local.entity.DbConstants
import com.study.wanandroid.data.local.entity.FavoriteArticleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    // 1. 首页 / 搜索页：观察已收藏的文章 ID 集合
    /**
     * 获取当前用户所有已收藏的文章 ID（排除待删除的）。
     * 首页和搜索页会收集这个 Flow，用来决定心形图标是实心还是空心。
     */
    @Query(
        """
        SELECT ${DbConstants.COL_FAV_ARTICLE_ID}
        FROM ${DbConstants.TABLE_FAVORITES}
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId
        AND ${DbConstants.COL_FAV_SYNC_STATE} != 'PENDING_DELETE'
    """
    )
    fun observeFavoriteIds(userId: Long): Flow<List<Long>>

    // 2. 收藏列表页：观察列表数据（支持分类过滤）
    /**
     * 获取当前用户的所有收藏文章（全部），用于"全部收藏"视图。
     * 排序规则：先按 serverOrder（本地新增为-1排最前），再按本地时间。
     */
    @Query(
        """
        SELECT * FROM ${DbConstants.TABLE_FAVORITES}
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId
        AND ${DbConstants.COL_FAV_SYNC_STATE} != 'PENDING_DELETE'
        ORDER BY ${DbConstants.COL_FAV_SERVER_ORDER} ASC, ${DbConstants.COL_FAV_COLLECT_TIME} DESC
    """
    )
    fun observeAllFavorites(userId: Long): Flow<List<FavoriteArticleEntity>>

    /**
     * 获取当前用户指定分类下的收藏文章。
     */
    @Query(
        """
        SELECT * FROM ${DbConstants.TABLE_FAVORITES} 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
          AND ${DbConstants.COL_FAV_CATEGORY_ID} = :categoryId
          AND ${DbConstants.COL_FAV_SYNC_STATE} != 'PENDING_DELETE'
        ORDER BY ${DbConstants.COL_FAV_SERVER_ORDER} ASC, ${DbConstants.COL_FAV_COLLECT_TIME} DESC
    """
    )
    fun observeFavoritesByCategory(
        userId: Long,
        categoryId: Long
    ): Flow<List<FavoriteArticleEntity>>

    /**
     * 获取当前用户"未分类"的收藏文章。
     */
    @Query(
        """
        SELECT * FROM ${DbConstants.TABLE_FAVORITES} 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
          AND ${DbConstants.COL_FAV_CATEGORY_ID} IS NULL
          AND ${DbConstants.COL_FAV_SYNC_STATE} != 'PENDING_DELETE'
        ORDER BY ${DbConstants.COL_FAV_SERVER_ORDER} ASC, ${DbConstants.COL_FAV_COLLECT_TIME} DESC
    """
    )
    fun observeUncategorizedFavorites(userId: Long): Flow<List<FavoriteArticleEntity>>

    //基础CRUD操作
    /**
     * 插入或覆盖（用于本地新增收藏、或远程分页拉取时整体覆盖）。
     * 注意：如果只是更新远程数据，最好在 Repository 里先查本地，合并 categoryId 和状态后再调用此方法。
     * upsert是room2.5.0版本引入，根据主键判断插入/更新。主键存在：更新；不存在：插入
     */
    @Upsert
    suspend fun upsert(favorite: FavoriteArticleEntity)

    /**
     * 获取单条记录（用于点击取消收藏时，判断当前状态）。
     */
    @Query(
        """
        SELECT * FROM ${DbConstants.TABLE_FAVORITES} 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
          AND ${DbConstants.COL_FAV_ARTICLE_ID} = :articleId
    """
    )
    suspend fun getFavorite(userId: Long, articleId: Long): FavoriteArticleEntity?

    /**
     * 物理删除（用于 PENDING_ADD 状态下的取消收藏，或 Worker 成功后清除 PENDING_DELETE 记录）。
     */
    @Query(
        """
        DELETE FROM ${DbConstants.TABLE_FAVORITES} 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
          AND ${DbConstants.COL_FAV_ARTICLE_ID} = :articleId
    """
    )
    suspend fun deleteById(userId: Long, articleId: Long)

    /**
     * 更新文章分类，或者移到未分类（传null）
     */
    @Query(
        """
        UPDATE ${DbConstants.TABLE_FAVORITES} 
        SET ${DbConstants.COL_FAV_CATEGORY_ID} = :categoryId 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
          AND ${DbConstants.COL_FAV_ARTICLE_ID} = :articleId
    """
    )
    suspend fun updateCategory(userId: Long, articleId: Long, categoryId: Long?)

    //4. 状态机流转
    //取消收藏（软删除，ui不显示该条记录是收藏的）
    @Query(
        """
        UPDATE ${DbConstants.TABLE_FAVORITES} 
        SET ${DbConstants.COL_FAV_SYNC_STATE} = 'PENDING_DELETE' 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
          AND ${DbConstants.COL_FAV_ARTICLE_ID} = :articleId
    """
    )
    suspend fun markAsPendingDelete(userId: Long, articleId: Long)

    //恢复收藏（后台请求成功，状态同步了）
    @Query(
        """
        UPDATE ${DbConstants.TABLE_FAVORITES} 
        SET ${DbConstants.COL_FAV_SYNC_STATE} = 'SYNCED', ${DbConstants.COL_FAV_LAST_ERROR} = NULL
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
          AND ${DbConstants.COL_FAV_ARTICLE_ID} = :articleId
    """
    )
    suspend fun markAsSynced(userId: Long, articleId: Long)

    //5. 供 WorkManager 后台同步使用
    /**
     * 手机刚联网时，获取所有待添加的记录（Worker 启动时扫表用）。
     */
    @Query(
        """
        SELECT * FROM ${DbConstants.TABLE_FAVORITES} 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
          AND ${DbConstants.COL_FAV_SYNC_STATE} = 'PENDING_ADD'
    """
    )
    suspend fun getPendingAdds(userId: Long): List<FavoriteArticleEntity>

    /**
     * 手机刚联网时，获取所有待删除的记录（Worker 启动时扫表用）。
     */
    @Query(
        """
        SELECT * FROM ${DbConstants.TABLE_FAVORITES} 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
          AND ${DbConstants.COL_FAV_SYNC_STATE} = 'PENDING_DELETE'
    """
    )
    suspend fun getPendingDeletes(userId: Long): List<FavoriteArticleEntity>

    /**
     * Worker 断网报错了（同步失败时），记录错误信息。
     */
    @Query(
        """
        UPDATE ${DbConstants.TABLE_FAVORITES} 
        SET ${DbConstants.COL_FAV_LAST_ERROR} = :error 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
          AND ${DbConstants.COL_FAV_ARTICLE_ID} = :articleId
    """
    )
    suspend fun setLastError(userId: Long, articleId: Long, error: String)

    // 6. 全量对账（基于 collectIds）
    /**
     * 获取本地所有状态为 SYNCED 的文章 ID（用于和远程 collectIds 比对）。
     */
    @Query(
        """
        SELECT ${DbConstants.COL_FAV_ARTICLE_ID} FROM ${DbConstants.TABLE_FAVORITES} 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
          AND ${DbConstants.COL_FAV_SYNC_STATE} = 'SYNCED'
    """
    )
    suspend fun getSyncedArticleIds(userId: Long): List<Long>

    /**
     * 清理本地存在但远程已取消（不在 keepIds 集合中）的 Synced 记录。
     * 注意：如果 keepIds 过大（超过 999），需要在 Repository 分批次调用，或者捞出来在内存里比对。
     */
    @Query(
        """
        DELETE FROM ${DbConstants.TABLE_FAVORITES} 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
          AND ${DbConstants.COL_FAV_SYNC_STATE} = 'SYNCED' 
          AND ${DbConstants.COL_FAV_ARTICLE_ID} NOT IN (:keepIds)
    """
    )
    suspend fun deleteSyncedNotIn(userId: Long, keepIds: List<Long>)

    //处理deleteSyncedNotIn中keepIds是空列表的情况
    @Query(
        """
        DELETE FROM ${DbConstants.TABLE_FAVORITES} 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
        AND ${DbConstants.COL_FAV_SYNC_STATE} = 'SYNCED'
    """
    )
    suspend fun deleteAllSynced(userId: Long)

    //7.计算全部文章数
    @Query(
        """
        SELECT COUNT(*) FROM ${DbConstants.TABLE_FAVORITES} 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
        AND ${DbConstants.COL_FAV_SYNC_STATE} != 'PENDING_DELETE'
    """
    )
    fun observeTotalFavoriteCount(userId: Long): Flow<Int>

    //8.计算未分类文章数
    @Query(
        """
        SELECT COUNT(*) FROM ${DbConstants.TABLE_FAVORITES} 
        WHERE ${DbConstants.COL_FAV_USER_ID} = :userId 
        AND ${DbConstants.COL_FAV_CATEGORY_ID} IS NULL 
        AND ${DbConstants.COL_FAV_SYNC_STATE} != 'PENDING_DELETE'    
    """
    )
    fun observeUncategorizedCount(userId: Long): Flow<Int>
}