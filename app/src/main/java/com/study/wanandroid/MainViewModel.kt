package com.study.wanandroid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.study.wanandroid.entity.AccountInfo
import com.study.wanandroid.entity.ArticleDetail
import com.study.wanandroid.entity.BannerImage
import com.study.wanandroid.entity.UserInfo
import com.study.wanandroid.network.ApiRepository
import com.study.wanandroid.network.PersistentCookieJar
import com.study.wanandroid.network.RetrofitClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: ApiRepository = ApiRepository()
    private val cookieJar: PersistentCookieJar = RetrofitClient.getCookieJar()

//    private val _articlesState = MutableStateFlow<ArticlesState>(ArticlesState.Loading)
//    val articlesState: StateFlow<ArticlesState> = _articlesState.asStateFlow()
//
//    fun getArticles(page: Int) {
//        viewModelScope.launch {
//            repository.getArticles(page)
//                .onSuccess {
//                    _articlesState.value = ArticlesState.Success(it.data.datas)
//                }
//                .onFailure {
//                    _articlesState.value= ArticlesState.Error(it.message?:"文章获取失败")
//                }
//        }
//    }

    //首页文章列表
    val articles: Flow<PagingData<ArticleDetail>> = repository
        .getArticles()
        //屏幕旋转后不会重新触发网络请求
        //多个 Composable 收集同一数据流时不会重复请求
        .cachedIn(viewModelScope)

    //搜索
    // null  = 没有搜索（或已清空）
    // 非 null = 触发对该关键词的搜索
    private val _searchKeyword = MutableStateFlow<String?>(null)

    //是否处于"已触发搜索"状态(解决第一次进入搜索界面时出现圆形指示器的问题)
    val isSearchActive: StateFlow<Boolean> = _searchKeyword
        .map { !it.isNullOrBlank() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val searchResults: Flow<PagingData<ArticleDetail>> = _searchKeyword
        //切换关键词时自动取消上一次搜索
        .flatMapLatest { keyword ->
            if (keyword.isNullOrBlank()) {
                // 空状态：返回空 PagingData，覆盖掉旧缓存
                flowOf(PagingData.empty())
            } else {
                repository.searchArticles(keyword)
            }
        }
        .cachedIn(viewModelScope)//缓存搜索结果

    /**
     * 触发搜索
     * 每次调用都会让 _searchKeyword 变成一个新的字符串，
     * flatMapLatest 就会重新触发搜索，并取消上一次
     */
    fun search(keyword: String) {
        if (keyword.isBlank()) return
        _searchKeyword.value = keyword
    }

    /**
     * 清空搜索状态
     * 把 _searchKeyword 设回 null，searchResults 会发出空数据
     */
    fun clearSearch() {
        _searchKeyword.value = null
    }

    //轮播图
    //重新加载触发器
    private val _bannerTrigger = MutableStateFlow(0)
    val bannerState: StateFlow<BannerState> = _bannerTrigger
        .flatMapLatest {
            repository.getBannerFlow()
        }
        .map { result ->
            result.fold(
                onSuccess = { BannerState.Success(it.data ?: emptyList()) },
                onFailure = { BannerState.Error(it.message ?: "轮播图获取失败") }
            )
        }
        .stateIn(
            scope = viewModelScope,
            //SharingStarted.Eagerly：立即启动，永不停止
            //SharingStarted.Lazily：第一个订阅者出现时启动，永不停止
            started = SharingStarted.WhileSubscribed(5000),//有订阅者时运行，最后一个订阅者离开后等 stopTimeoutMillis 再停止
            initialValue = BannerState.Loading
        )

    fun retryBanner() {
        _bannerTrigger.value++
    }

    //是否为已登录的状态（初始值通过cookie判断）
    private val _isLoggedIn = MutableStateFlow(cookieJar.hasCookies())
    val isLogged: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    //登录结果状态（用作ui的提示信息）
    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    //存储当前登录用户的个人信息
    private val _accountInfo = MutableStateFlow<AccountInfo?>(null)
    val accountInfo: StateFlow<AccountInfo?> = _accountInfo.asStateFlow()

    //登录
    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.login(username, password)
                .onSuccess { userInfo ->
                    _isLoggedIn.value = true//更新登录状态
                    //_userInfo.value = userInfo
                    _authState.value = AuthState.Success(userInfo)
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "登录失败")
                }
        }
    }

    //todo:init块只有当这个实例被创建时会被执行
    //当前用户信息以这种方式执行，退出再登录时，由于viewmodel实例已存在，init块不执行，故没有accountInfo
//    init {
//        // 如果发现本地有 Cookie，说明已登录，自动去请求一次用户信息
//        if (cookieJar.hasCookies()) {
//            fetchAccountInfo()
//        }
//    }

    fun fetchAccountInfo() {
        viewModelScope.launch {
            repository.getAccountInfo()
                .onSuccess { accountInfo ->
                    _accountInfo.value = accountInfo
                }
                .onFailure {
                    // 如果获取失败（比如 Cookie 过期了），就把登录态清掉
                    cookieJar.clear()
                    _isLoggedIn.value = false
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
                cookieJar.clear()//退出成功时清理本地cookie
                _isLoggedIn.value = false
                _accountInfo.value = null
                _authState.value = AuthState.Success(null)
            }
        }
    }
}

//sealed class ArticlesState{
//    object Loading: ArticlesState()
//    data class Success(val articles: List<ArticleDetail>): ArticlesState()
//    data class Error(val message: String): ArticlesState()
//}

//轮播图
sealed class BannerState {
    object Loading : BannerState()
    data class Success(val banner: List<BannerImage>) : BannerState()
    data class Error(val message: String) : BannerState()
}

//认证状态
sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val userInfo: UserInfo? = null) : AuthState()
    object RegisterSuccess : AuthState()
    data class Error(val message: String) : AuthState()
}