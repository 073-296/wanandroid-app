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





