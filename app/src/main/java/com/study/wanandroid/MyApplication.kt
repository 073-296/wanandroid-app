package com.study.wanandroid

import android.app.Application
import com.study.wanandroid.db.AppDatabase
import com.study.wanandroid.network.RetrofitClient

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        RetrofitClient.init(this)
        ToastUtils.init(this)
        AppDatabase.init(this)
    }
}