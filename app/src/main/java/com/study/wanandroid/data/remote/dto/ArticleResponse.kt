package com.study.wanandroid.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ArticleResponse(
    val datas: List<ArticleDetail>
)

@Serializable
data class ArticleDetail(
    val author: String = "",
    val collect: Boolean = false,
    val id: Int = 0,
    val link: String = "",
    val title: String = "",
    val shareUser: String = "",
    val niceDate: String = ""
)