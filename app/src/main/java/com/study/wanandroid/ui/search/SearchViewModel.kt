package com.study.wanandroid.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.study.wanandroid.data.local.AppDatabase
import com.study.wanandroid.data.local.entity.FavoriteArticleEntity
import com.study.wanandroid.data.remote.RetrofitClient
import com.study.wanandroid.data.remote.dto.ArticleDetail
import com.study.wanandroid.data.repository.ApiRepository
import com.study.wanandroid.data.repository.FavoriteRepository
import com.study.wanandroid.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

//搜索：关键词驱动的分页搜索，以及搜索结果列表的收藏状态
class SearchViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: ApiRepository = ApiRepository()
    private val session = UserSessionRepository.getInstance()

    private val db = AppDatabase.getInstance()
    private val favoriteRepo = FavoriteRepository(
        context = application,
        apiService = RetrofitClient.apiService,
        favoriteDao = db.favoriteDao(),
        categoryDao = db.categoryDao()
    )

    // null  = 没有搜索（或已清空）
    // 非 null = 触发对该关键词的搜索
    private val _searchKeyword = MutableStateFlow<String?>(null)

    //是否处于"已触发搜索"状态(解决第一次进入搜索界面时出现圆形指示器的问题)
    val isSearchActive: StateFlow<Boolean> = _searchKeyword
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val searchResults: Flow<PagingData<ArticleDetail>> = _searchKeyword
        //切换关键词时自动取消上一次搜索
        .flatMapLatest { keyword ->
            if (keyword.isNullOrBlank()) {
                // 空状态：返回空 PagingData，覆盖掉旧缓存
                flowOf(PagingData.empty())
            } else {
                repository.searchArticles(keyword)
            }
        }
        .cachedIn(viewModelScope)//缓存搜索结果

    /**
     * 触发搜索
     * 每次调用都会让 _searchKeyword 变成一个新的字符串，
     * flatMapLatest 就会重新触发搜索，并取消上一次
     */
    fun search(keyword: String) {
        if (keyword.isBlank()) return
        _searchKeyword.value = keyword
    }

    /**
     * 清空搜索状态
     * 把 _searchKeyword 设回 null，searchResults 会发出空数据
     */
    fun clearSearch() {
        _searchKeyword.value = null
    }

    //当前用户 id（ArticleItem 构造收藏实体用）
    val currentUserId: StateFlow<Long?> = session.currentUserId

    //搜索结果列表的收藏状态（与 HomeViewModel 各自订阅同一 Room 数据源）
    val favoriteIds: StateFlow<Set<Long>> = session.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptySet())
            else favoriteRepo.observeFavoriteIds(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    //点击收藏图标
    fun toggleFavorite(article: FavoriteArticleEntity) {
        val userId = session.currentUserId.value ?: return
        viewModelScope.launch {
            favoriteRepo.toggleFavorite(userId, article)
        }
    }
}
