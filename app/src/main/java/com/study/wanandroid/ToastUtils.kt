package com.study.wanandroid

import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast

@SuppressLint("StaticFieldLeak")
object ToastUtils {
    private var toast: Toast? = null
    private lateinit var context: Context

    //在application中初始化
    fun init(appContext: Context) {
        context = appContext
    }

    //短Toast
    fun show(message: String) {
        toast?.cancel()//取消上一个，防止出现排队现象
        toast = Toast.makeText(context, message, Toast.LENGTH_SHORT).apply { show() }
    }

    //长Toast
    fun showLong(message: String) {
        toast?.cancel()//取消上一个，防止出现排队现象
        toast = Toast.makeText(context, message, Toast.LENGTH_LONG).apply { show() }
    }

}