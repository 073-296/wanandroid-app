package com.study.wanandroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.study.wanandroid.entity.ArticleDetail
import com.study.wanandroid.entity.BannerImage
import com.study.wanandroid.network.ApiRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ApiRepository = ApiRepository()
) : ViewModel() {

//    private val _articlesState = MutableStateFlow<ArticlesState>(ArticlesState.Loading)
//    val articlesState: StateFlow<ArticlesState> = _articlesState.asStateFlow()
//
//    fun getArticles(page: Int) {
//        viewModelScope.launch {
//            repository.getArticles(page)
//                .onSuccess {
//                    _articlesState.value = ArticlesState.Success(it.data.datas)
//                }
//                .onFailure {
//                    _articlesState.value= ArticlesState.Error(it.message?:"文章获取失败")
//                }
//        }
//    }

    //首页文章列表
    val articles: Flow<PagingData<ArticleDetail>> = repository
        .getArticles()
        //屏幕旋转后不会重新触发网络请求
        //多个 Composable 收集同一数据流时不会重复请求
        .cachedIn(viewModelScope)

    //搜索
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

    //轮播图
    //重新加载触发器
    private val _bannerTrigger = MutableStateFlow(0)
    val bannerState: StateFlow<BannerState> = _bannerTrigger
        .flatMapLatest {
            repository.getBannerFlow()
        }
        .map { result ->
            result.fold(
                onSuccess = { BannerState.Success(it.data) },
                onFailure = { BannerState.Error(it.message ?: "轮播图获取失败") }
            )
        }
        .stateIn(
            scope = viewModelScope,
            //SharingStarted.Eagerly：立即启动，永不停止
            //SharingStarted.Lazily：第一个订阅者出现时启动，永不停止
            started = SharingStarted.WhileSubscribed(5000),//有订阅者时运行，最后一个订阅者离开后等 stopTimeoutMillis 再停止
            initialValue = BannerState.Loading
        )

    fun retryBanner(){
        _bannerTrigger.value++
    }
}

//sealed class ArticlesState{
//    object Loading: ArticlesState()
//    data class Success(val articles: List<ArticleDetail>): ArticlesState()
//    data class Error(val message: String): ArticlesState()
//}

//轮播图
sealed class BannerState {
    object Loading : BannerState()
    data class Success(val banner: List<BannerImage>) : BannerState()
    data class Error(val message: String) : BannerState()
}