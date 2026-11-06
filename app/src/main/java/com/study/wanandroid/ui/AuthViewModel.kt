package com.study.wanandroid.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.study.wanandroid.data.remote.SessionEvents
import com.study.wanandroid.data.remote.dto.AccountInfo
import com.study.wanandroid.data.remote.dto.UserInfo
import com.study.wanandroid.data.repository.ApiRepository
import com.study.wanandroid.data.repository.UserSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

//登录注册/账号会话：登录态与用户信息本体在 UserSessionRepository，这里只负责"动作"与提示状态
class AuthViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: ApiRepository = ApiRepository()
    private val session = UserSessionRepository.getInstance()

    val isLogged: StateFlow<Boolean> = session.isLoggedIn
    val accountInfo: StateFlow<AccountInfo?> = session.accountInfo

    //登录/注册结果状态（用作ui的提示信息）
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        //发生未授权（401 / errorCode -1001）时，会话已被 UserSessionRepository 清空，
        //这里只需把提示状态复位
        viewModelScope.launch {
            SessionEvents.onUnauthorized.collect {
                _authState.value = AuthState.Idle
            }
        }
    }

    //登录
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.login(username, password)
                .onSuccess { userInfo ->
                    session.onLoginSuccess()//更新登录态并拉取用户信息
                    _authState.value = AuthState.Success(userInfo)
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "登录失败")
                }
        }
    }

    //注册
    fun register(username: String, password: String, repassword: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.register(username, password, repassword)
                .onSuccess {
                    _authState.value = AuthState.RegisterSuccess//注册成功，跳转到登录
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "注册失败")
                }
        }
    }

    //退出登录
    fun logout() {
        viewModelScope.launch {
            repository.logout().onSuccess {
                //取消该用户所有排队中的同步任务
                val userIdToCancel = session.currentUserId.value
                if (userIdToCancel != null) {
                    WorkManager.getInstance(getApplication())
                        .cancelAllWorkByTag("user_$userIdToCancel")
                }

                session.clearSession()//清理登录态与本地cookie
                _authState.value = AuthState.Success(null)
            }
        }
    }

    fun fetchAccountInfo() {
        session.fetchAccountInfo()
    }
}

//认证状态
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userInfo: UserInfo? = null) : AuthState()
    object RegisterSuccess : AuthState()
    data class Error(val message: String) : AuthState()
}
