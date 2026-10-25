package com.study.wanandroid.entity

import kotlinx.serialization.Serializable

// 通用响应包装
@Serializable
data class ApiResponse<T>(
    val errorCode: Int,
    val errorMsg: String,
    val data: T? = null
)





