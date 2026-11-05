package com.study.wanandroid.entity

import kotlinx.serialization.Serializable

@Serializable
data class AccountInfo(
    val coinInfo: CoinInfo,
    val collectArticleInfo: CollectArticleInfo,
    val userInfo: UserInfo
)

@Serializable
data class CoinInfo(
    val coinCount: Int = 0,
    val level: Int = 0,
    val nickname: String = "",
    val rank: String = "",
    val username: String = ""//用户名不能详细显示
)

@Serializable
data class CollectArticleInfo(
    val count: Int = 0
)

@Serializable
data class UserInfo(
    val admin: Boolean = false,
    val coinCount: Int = 0,
    val collectIds: List<Long> = emptyList(),
    val id: Long = 0L,
    //三者结果一致，用户名能详细显示
    val nickname: String = "",
    val publicName: String = "",
    val username: String = ""
)