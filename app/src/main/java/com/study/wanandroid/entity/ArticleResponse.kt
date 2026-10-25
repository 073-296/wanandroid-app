package com.study.wanandroid.entity

import kotlinx.serialization.Serializable


// 通用响应包装
@Serializable
data class ApiResponse<T>(
    val errorCode: Int,
    val errorMsg: String,
    val data: T? = null
)

// 登录返回的用户信息
@Serializable
data class UserInfo(
    val id: Int = 0,
    val username: String = "",
    val nickname: String = "",
    val icon: String = "",
    val coinCount: Int = 0
)

// 文章列表响应
@Serializable
data class ArticleResponse(
    val data: ResponseData,
    val errorCode: Int,
    val errorMsg: String
)

@Serializable
data class ResponseData(
    val datas: List<ArticleDetail>
)

@Serializable
data class ArticleDetail(
    val author: String = "",
    val collect: Boolean = false,
    val id: Int = 0,
    val link: String = "",
    val title: String = "",
    val shareUser: String = ""
)



