package com.study.wanandroid

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

//在应用内借用户的浏览器内核来展示网页
fun customTab(context: Context,url:String){
    val builder = CustomTabsIntent.Builder()
    //可以通过builder.set...来设计网页样式

    val customTabsIntent=builder.build()
    customTabsIntent.launchUrl(context, url.toUri())

}