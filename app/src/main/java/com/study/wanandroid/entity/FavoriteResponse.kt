package com.study.wanandroid.entity

import kotlinx.serialization.Serializable

//收藏文章列表响应体
@Serializable
data class FavoriteResponse(
    val curPage: Int,
    val datas: List<FavoriteDetail>,
    val offset: Int,
    val over: Boolean,
    val pageCount: Int,
    val size: Int,
    val total: Int
)

@Serializable
data class FavoriteDetail(
    val author: String = "",
    val link: String,
    val niceDate: String = "",
    val originId: Long,
    val publishTime: Long = 0L,
    val title: String,
    val userId: Long
)