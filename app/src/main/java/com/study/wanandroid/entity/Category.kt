package com.study.wanandroid.entity

//收藏界面的侧边栏展示分类名的实体
data class Category(
    val id: Long?,
    val name: String,
    val count: Int,
    val isRealCategory: Boolean//判断是否隐藏重命名/删除按钮
)
