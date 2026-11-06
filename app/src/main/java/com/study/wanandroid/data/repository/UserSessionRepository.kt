package com.study.wanandroid.data.repository

import com.study.wanandroid.data.remote.PersistentCookieJar
import com.study.wanandroid.data.remote.RetrofitClient
import com.study.wanandroid.data.remote.SessionEvents
import com.study.wanandroid.data.remote.dto.AccountInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 进程级单例：持有登录态、用户信息与派生的 currentUserId。
 * 所有 ViewModel 都从这里读会话状态，避免跨 ViewModel 依赖。
 */
class UserSessionRepository private constructor(
    private val repository: ApiRepository,
    private val cookieJar: PersistentCookieJar
) {

    //会话状态要活得比任何 ViewModel 都长，用自建 scope 而不是 viewModelScope
    private val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    //是否为已登录的状态（初始值通过cookie判断）
    private val _isLoggedIn = MutableStateFlow(cookieJar.hasAuthCookie())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    //存储当前登录用户的个人信息
    private val _accountInfo = MutableStateFlow<AccountInfo?>(null)
    val accountInfo: StateFlow<AccountInfo?> = _accountInfo.asStateFlow()

    //获取当前userId
    //退出时，accountInfo变为null，userId也会变为null
    val currentUserId: StateFlow<Long?> = _accountInfo
        .map { it?.userInfo?.id?.toLong() }
        .stateIn(sessionScope, SharingStarted.Eagerly, null)

    init {
        // 监听全局未授权事件
        sessionScope.launch {
            SessionEvents.onUnauthorized.collect {
                // 发生未授权，清空 Cookie，重置状态
                clearSession()
            }
        }
        //若启动时已有cookie，主动拉取用户信息
        if (cookieJar.hasAuthCookie()) {
            fetchAccountInfo()
        }
    }

    //登录成功后由 AuthViewModel 调用
    fun onLoginSuccess() {
        _isLoggedIn.value = true
        fetchAccountInfo()
    }

    //清除登录态（退出登录 / 未授权踢出 共用）
    fun clearSession() {
        cookieJar.clear()
        _isLoggedIn.value = false
        _accountInfo.value = null
    }

    fun fetchAccountInfo() {
        sessionScope.launch {
            repository.getAccountInfo()
                .onSuccess { accountInfo ->
                    _accountInfo.value = accountInfo
                }
                .onFailure {
                    //依靠全局拦截器处理失败情况
                }
        }
    }

    companion object {
        @Volatile
        private var instance: UserSessionRepository? = null

        fun getInstance(): UserSessionRepository =
            instance ?: synchronized(this) {
                instance ?: UserSessionRepository(
                    repository = ApiRepository(),
                    cookieJar = RetrofitClient.getCookieJar()
                ).also { instance = it }
            }
    }
}
