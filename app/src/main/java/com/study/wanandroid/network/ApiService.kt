package com.study.wanandroid.network

import com.study.wanandroid.entity.ArticleResponse
import com.study.wanandroid.entity.BannerResponse
import com.study.wanandroid.entity.LoginRequest
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
    ): LoginRequest

    // 获取文章列表
    @GET("article/list/{page}/json")
    suspend fun getArticles(
        @Path("page") page: Int = 0
    ): ArticleResponse

    //轮播图
    @GET("banner/json")
    suspend fun getBanner(): BannerResponse
}