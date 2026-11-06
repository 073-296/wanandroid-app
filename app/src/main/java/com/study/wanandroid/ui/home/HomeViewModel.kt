package com.study.wanandroid.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.study.wanandroid.data.local.AppDatabase
import com.study.wanandroid.data.local.entity.FavoriteArticleEntity
import com.study.wanandroid.data.remote.RetrofitClient
import com.study.wanandroid.data.remote.dto.ArticleDetail
import com.study.wanandroid.data.remote.dto.BannerImage
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

//首页：文章分页、轮播图，以及文章列表的收藏状态
class HomeViewModel(
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

    //首页文章列表
    val articles: Flow<PagingData<ArticleDetail>> = repository
        .getArticles()
        //屏幕旋转后不会重新触发网络请求
        //多个 Composable 收集同一数据流时不会重复请求
        .cachedIn(viewModelScope)

    //轮播图
    //重新加载触发器
    private val _bannerTrigger = MutableStateFlow(0)
    val bannerState: StateFlow<BannerState> = _bannerTrigger
        .flatMapLatest {
            repository.getBannerFlow()
        }
        .map { result ->
            result.fold(
                onSuccess = { BannerState.Success(it.data ?: emptyList()) },
                onFailure = { BannerState.Error(it.message ?: "轮播图获取失败") }
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),//有订阅者时运行，最后一个订阅者离开后等 stopTimeoutMillis 再停止
            initialValue = BannerState.Loading
        )

    fun retryBanner() {
        _bannerTrigger.value++
    }

    //当前用户 id（ArticleItem 构造收藏实体用）
    val currentUserId: StateFlow<Long?> = session.currentUserId

    //首页/搜索页：全局收藏状态(哪些文章该显示"已收藏")
    //flatMapLatest:账号切换时自动重新查询数据库
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

//轮播图
sealed class BannerState {
    object Loading : BannerState()
    data class Success(val banner: List<BannerImage>) : BannerState()
    data class Error(val message: String) : BannerState()
}
