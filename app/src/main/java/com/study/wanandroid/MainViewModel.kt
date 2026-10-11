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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        getBanner()
    }

    //首页文章列表
    val articles: Flow<PagingData<ArticleDetail>> = repository
        .getArticles()
        //屏幕旋转后不会重新触发网络请求
        //多个 Composable 收集同一数据流时不会重复请求
        .cachedIn(viewModelScope)

    //轮播图
    private val _bannerState = MutableStateFlow<BannerState>(BannerState.Loading)
    val bannerState: StateFlow<BannerState> = _bannerState.asStateFlow()

    fun getBanner() {
        viewModelScope.launch {
            repository.getBanner()
                .onSuccess {
                    _bannerState.value = BannerState.Success(it.data)
                }
                .onFailure {
                    _bannerState.value = BannerState.Error(it.message ?: "轮播图获取失败")
                }
        }
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