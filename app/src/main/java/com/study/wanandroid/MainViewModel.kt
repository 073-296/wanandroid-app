package com.study.wanandroid

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.study.wanandroid.entity.ArticleDetail
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

    val articles: Flow<PagingData<ArticleDetail>> = repository
        .getArticles()
        //屏幕旋转后不会重新触发网络请求
        //多个 Composable 收集同一数据流时不会重复请求
        .cachedIn(viewModelScope)
}

//sealed class ArticlesState{
//    object Loading: ArticlesState()
//    data class Success(val articles: List<ArticleDetail>): ArticlesState()
//    data class Error(val message: String): ArticlesState()
//}