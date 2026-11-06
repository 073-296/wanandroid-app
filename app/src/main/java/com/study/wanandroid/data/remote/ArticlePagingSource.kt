package com.study.wanandroid.data.remote

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.study.wanandroid.data.remote.dto.ArticleDetail

class ArticlePagingSource(
    private val api: ApiService,
    private val keyword: String? = null
) : PagingSource<Int, ArticleDetail>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ArticleDetail> {
        // params.key 是当前页码，首次加载为 null
        val page = params.key ?: 0
        return try {
            val response = if (keyword.isNullOrBlank()) api.getArticles(page = page)
            else api.searchArticles(keyWord = keyword, page = page)

            if (response.errorCode == 0) {
                val articles = response.data?.datas ?: emptyList()
                LoadResult.Page(
                    data = articles,
                    prevKey = if (page == 0) null else page - 1,
                    nextKey = if (articles.isEmpty()) null else page + 1
                )
            } else {
                LoadResult.Error(Exception(response.errorMsg))
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, ArticleDetail>): Int? {
        // 刷新时恢复到之前的位置
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition = anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition = anchorPosition)?.nextKey?.minus(1)
        }
    }
}