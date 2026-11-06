package com.study.wanandroid

import android.app.Application
import com.study.wanandroid.common.ToastUtils
import com.study.wanandroid.data.local.AppDatabase
import com.study.wanandroid.data.remote.RetrofitClient

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        ToastUtils.init(this)
        AppDatabase.init(this)
    }
}