package com.study.wanandroid

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

//在应用内借用户的浏览器内核来展示网页
fun customTab(context: Context,url:String){
    //服务端对自家域名下的文章只返回相对路径（如 /blog/show/3938），
    //没有 scheme 的 Uri 无法被浏览器打开，这里统一补全为完整 URL
    val fullUrl = if (url.toUri().scheme == null) {
        "https://wanandroid.com/${url.trimStart('/')}"
    } else {
        url
    }
    val builder = CustomTabsIntent.Builder()
    //可以通过builder.set...来设计网页样式

    val customTabsIntent=builder.build()
    customTabsIntent.launchUrl(context, fullUrl.toUri())

}