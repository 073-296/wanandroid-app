package com.study.wanandroid.network

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object SessionEvents {
    // 使用 SharedFlow 发送一次性事件
    private val _onUnauthorized = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val onUnauthorized: SharedFlow<Unit> = _onUnauthorized.asSharedFlow()

    fun notifyUnauthorized() {
        _onUnauthorized.tryEmit(Unit)
    }
}