package com.study.wanandroid.ui.collect

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.study.wanandroid.common.ToastUtils
import com.study.wanandroid.data.local.AppDatabase
import com.study.wanandroid.data.local.entity.FavoriteArticleEntity
import com.study.wanandroid.data.remote.RetrofitClient
import com.study.wanandroid.data.repository.ApiRepository
import com.study.wanandroid.data.repository.FavoriteRepository
import com.study.wanandroid.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

//收藏页：分类筛选、分类管理、收藏列表与全量对账
class CollectViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val session = UserSessionRepository.getInstance()

    private val db = AppDatabase.getInstance()
    private val favoriteRepo = FavoriteRepository(
        context = application,
        apiService = RetrofitClient.apiService,
        favoriteDao = db.favoriteDao(),
        categoryDao = db.categoryDao()
    )

    //当前选中的分类（null为全部，-1L为未分类）
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    //分类列表及数量
    val sidebarCategories: StateFlow<List<Category>> = session.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                combine(
                    favoriteRepo.observeCategoriesWithCount(userId),//每个真实分类对应文章数量
                    favoriteRepo.observeTotalFavoriteCount(userId),//全部文章数量
                    favoriteRepo.observeUncategorizedCount(userId)//未分类文章数量
                ) { realCategories, totalCount, uncategorizedCount ->
                    val list = mutableListOf<Category>()
                    // 1. 添加"全部"
                    list.add(Category(null, "全部收藏", totalCount, false))
                    // 2. 添加"未分类"
                    list.add(Category(-1L, "未分类", uncategorizedCount, false))
                    // 3. 循环添加真实分类
                    realCategories.forEach {
                        list.add(Category(it.id, it.name, it.articleCount, true))
                    }
                    list
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //分类表下的文章
    val favoriteList: StateFlow<List<FavoriteArticleEntity>> = combine(
        session.currentUserId,
        _selectedCategoryId
    ) { userId, catId ->
        userId to catId
    }.flatMapLatest { (userId, catId) ->
        if (userId == null) {
            flowOf(emptyList())
        } else {
            //sidebarCategories变量处约定：
            //catId == null，全部
            //catId == -1L，未分类
            //catId >0，某个分类
            when (catId) {
                null -> favoriteRepo.observeFavorites(userId, null)
                -1L -> favoriteRepo.observeUncategorizedFavorites(userId)
                else -> favoriteRepo.observeFavorites(userId, catId)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //点击收藏图标
    fun toggleFavorite(article: FavoriteArticleEntity) {
        val userId = session.currentUserId.value ?: return
        viewModelScope.launch {
            favoriteRepo.toggleFavorite(userId, article)
        }
    }

    //添加分类
    fun addCategory(name: String, onResult: (Result<Unit>) -> Unit) {
        val userId = session.currentUserId.value ?: return
        viewModelScope.launch {
            val result = favoriteRepo.addCategory(userId, name)
            onResult(result)
        }
    }

    //删除分类
    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch { favoriteRepo.deleteCategory(categoryId) }
    }

    //将文章添加至分类
    fun setArticleCategory(articleId: Long, categoryId: Long?) {
        val userId = session.currentUserId.value ?: return
        viewModelScope.launch {
            favoriteRepo.setArticleCategory(userId, articleId, categoryId)
                .onSuccess {
                    ToastUtils.show("操作成功ヽ(✿ﾟ▽ﾟ)ノ")
                }
                .onFailure {
                    ToastUtils.show(it.message ?: "操作失败≧ ﹏ ≦")
                }
        }
    }

    //分类重命名
    fun renameCategory(categoryId: Long, newName: String, onResult: (Result<Unit>) -> Unit) {
        val userId = session.currentUserId.value ?: return
        viewModelScope.launch {
            val result = favoriteRepo.renameCategory(userId, categoryId, newName)
            onResult(result)
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    //全量对账刷新
    fun refreshFavorites() {
        val userId = session.currentUserId.value ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                favoriteRepo.refreshAll(userId)
            } catch (e: Exception) {
                e.printStackTrace()
                ToastUtils.show("刷新失败:${e.message?:"网络异常"}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }
}
