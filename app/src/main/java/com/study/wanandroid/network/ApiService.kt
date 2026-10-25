package com.study.wanandroid.network

import com.study.wanandroid.entity.AccountInfo
import com.study.wanandroid.entity.ApiResponse
import com.study.wanandroid.entity.ArticleResponse
import com.study.wanandroid.entity.BannerImage
import com.study.wanandroid.entity.UserInfo
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {
    //登录
    @FormUrlEncoded
    @POST("user/login")
    suspend fun login(
        @Field("username") username: String,
        @Field("password") password: String
    ): ApiResponse<UserInfo>

    //注册
    @FormUrlEncoded
    @POST("user/register")
    suspend fun register(
        @Field("username") username:String,
        @Field("password") password:String,
        @Field("repassword") repassword:String
    ): ApiResponse<Unit>

    //退出登录
    @GET("user/logout/json")
    suspend fun logout(): ApiResponse<Unit>

    // 获取文章列表
    @GET("article/list/{page}/json")
    suspend fun getArticles(
        @Path("page") page: Int = 0
    ): ApiResponse<ArticleResponse>

    //轮播图
    @GET("banner/json")
    suspend fun getBanner(): ApiResponse<List<BannerImage>>

    //搜索
    @FormUrlEncoded
    @POST("article/query/{page}/json")
    suspend fun searchArticles(
        @Path("page") page:Int,
        @Field("k") keyWord:String
    ): ApiResponse<ArticleResponse>

    //用户积分，收藏列表数、id，用户信息
    @GET("/user/lg/userinfo/json")
    suspend fun getAccountInfo(): ApiResponse<AccountInfo>
}