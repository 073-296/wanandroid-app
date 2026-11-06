package com.study.wanandroid.data.remote.dto

import kotlinx.serialization.Serializable

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