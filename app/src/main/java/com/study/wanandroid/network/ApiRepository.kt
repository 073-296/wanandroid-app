package com.study.wanandroid.network

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.study.wanandroid.entity.ApiResponse
import com.study.wanandroid.entity.ArticleDetail
import com.study.wanandroid.entity.BannerImage
import com.study.wanandroid.entity.UserInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ApiRepository(
    private val api: ApiService = RetrofitClient.apiService
) {
    //获取首页文章列表
//    suspend fun getArticles(page: Int): Result<ArticleResponse> {
//        return try {
//            val response = api.getArticles(page)
//            if (response.errorCode==0) {
//                Result.success(response)
//            }else{
//                Result.failure(Exception(response.errorMsg))
//            }
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
    //获取首页文章列表
    fun getArticles(): Flow<PagingData<ArticleDetail>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,//每页加载的数量
                prefetchDistance = 3,//距离底部3条时预加载
                enablePlaceholders = false,//不显示占位符
                initialLoadSize = 20//首次加载的数量
            ),
            //pagingSourceFactory每次都要返回新实例，不能复用
            pagingSourceFactory = { ArticlePagingSource(api) }
        ).flow
    }

    //搜索文章
    fun searchArticles(keyword: String): Flow<PagingData<ArticleDetail>> {
        return Pager(
            config = PagingConfig(
                pageSize = 20,
                prefetchDistance = 3,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { ArticlePagingSource(api, keyword) }
        ).flow
    }

    //获取轮播图
    //把请求数据变成数据流
    fun getBannerFlow(): Flow<Result<ApiResponse<List<BannerImage>>>> = flow {
        emit(getBanner())
    }

    suspend fun getBanner(): Result<ApiResponse<List<BannerImage>>> {
        return try {
            val response = api.getBanner()
            if (response.errorCode == 0) {
                Result.success(response)
            } else {
                Result.failure(Exception(response.errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //登录
    suspend fun login(username: String, password: String): Result<UserInfo> {
        return try {
            val response = api.login(username, password)
            if (response.errorCode == 0 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //注册
    suspend fun register(username: String, password: String, repassword: String): Result<Unit> {
        return try {
            val response = api.register(username, password, repassword)
            if (response.errorCode == 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //退出登录
    suspend fun logout(): Result<Unit> {
        return try {
            val response = api.logout()
            if (response.errorCode == 0) {
                Result.success(Unit)
            } else {
                Result.failure(Exception(response.errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    //用户积分、用户名
    suspend fun getUserInfo(): Result<UserInfo> {
        return try {
            val response = api.getUserInfo()
            if (response.errorCode == 0 && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(Exception(response.errorMsg))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}