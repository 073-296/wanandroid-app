package com.study.wanandroid.entity

import kotlinx.serialization.Serializable

@Serializable
data class BannerResponse(
    val data: List<BannerImage>,
    val errorCode: Int,
    val errorMsg: String
)

@Serializable
data class BannerImage(
    val desc: String = "",
    val id: Int = -1,
    val imagePath: String = "",
    val isVisible: Int = -1,
    val order: Int = -1,
    val title: String = "",
    val type: Int = -1,
    val url: String = ""
)