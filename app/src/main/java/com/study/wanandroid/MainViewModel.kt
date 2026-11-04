package com.study.wanandroid

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.work.WorkManager
import com.study.wanandroid.db.AppDatabase
import com.study.wanandroid.db.CategoryWithCount
import com.study.wanandroid.db.FavoriteArticleEntity
import com.study.wanandroid.db.FavoriteRepository
import com.study.wanandroid.entity.AccountInfo
import com.study.wanandroid.entity.ArticleDetail
import com.study.wanandroid.entity.BannerImage
import com.study.wanandroid.entity.Category
import com.study.wanandroid.entity.UserInfo
import com.study.wanandroid.network.ApiRepository
import com.study.wanandroid.network.PersistentCookieJar
import com.study.wanandroid.network.RetrofitClient
import com.study.wanandroid.network.SessionEvents
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    private val db = AppDatabase.getInstance()
    private val favoriteRepo = FavoriteRepository(
        context = application,
        apiService = RetrofitClient.apiService,
        favoriteDao = db.favoriteDao(),
        categoryDao = db.categoryDao()
    )

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
    private val _isLoggedIn = MutableStateFlow(cookieJar.hasAuthCookie())
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
                    //登录成功后立刻获取用户信息
                    fetchAccountInfo()
                    _authState.value = AuthState.Success(userInfo)
                }
                .onFailure {
                    _authState.value = AuthState.Error(it.message ?: "登录失败")
                }
        }
    }

    //init块只有当这个实例被创建时会被执行
    init {
        // 监听全局未授权事件
        viewModelScope.launch {
            SessionEvents.onUnauthorized.collect {
                // 发生未授权，清空 Cookie，重置状态
                cookieJar.clear()
                _isLoggedIn.value = false
                _accountInfo.value = null
                _authState.value = AuthState.Idle // 或者提示“登录已过期，请重新登录”
            }
        }
        //若启动时已有cookie，主动拉取用户信息
        if (cookieJar.hasAuthCookie()) {
            fetchAccountInfo()
        }
    }

    fun fetchAccountInfo() {
        viewModelScope.launch {
            repository.getAccountInfo()
                .onSuccess { accountInfo ->
                    _accountInfo.value = accountInfo
                }
                .onFailure {
                    //依靠全局拦截器处理失败情况
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
                val userIdToCancel = currentUserId.value
                if (userIdToCancel != null) {
                    WorkManager.getInstance(getApplication())
                        .cancelAllWorkByTag("user_$userIdToCancel")
                }

                cookieJar.clear()//退出成功时清理本地cookie
                _isLoggedIn.value = false
                _accountInfo.value = null
                _authState.value = AuthState.Success(null)
            }
        }
    }

    //获取当前userId
    //退出时，accountInfo变为null，userId也会变为null
    val currentUserId: StateFlow<Long?> = _accountInfo
        .map { it?.userInfo?.id?.toLong() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    //首页/搜索页：全局搜索状态(哪些文章该显示“已收藏”)
    //flatMapLatest:账号切换时自动重新查询数据库
    val favoriteIds: StateFlow<Set<Long>> = currentUserId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptySet())
            else favoriteRepo.observeFavoriteIds(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    //当前选中的分类（null为全部，-1L为未分类）
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    //分类列表及数量
    val sidebarCategories: StateFlow<List<Category>> = currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptyList())
            } else {
                combine(
                    favoriteRepo.observeCategoriesWithCount(userId),//每个真实分类对应文章数量
                    favoriteRepo.observeTotalFavoriteCount(userId),//全部文章数量
                    favoriteRepo.observeUncategorizedCount(userId)//未分类文章数量
                ) { realCategories, totalCount, uncategorizedCount ->
                    val list = mutableListOf<Category>()
                    // 1. 添加“全部”
                    list.add(Category(null, "全部收藏", totalCount, false))
                    // 2. 添加“未分类”
                    list.add(Category(-1L, "未分类", uncategorizedCount, false))
                    // 3. 循环添加真实分类
                    realCategories.forEach {
                        list.add(Category(it.id, it.name, it.articleCount, true))
                    }
                    list
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //分类表下的文章
    val favoriteList: StateFlow<List<FavoriteArticleEntity>> = combine(
        currentUserId,
        _selectedCategoryId
    ) { userId, catId ->
        userId to catId
    }.flatMapLatest { (userId, catId) ->
        if (userId == null) {
            flowOf(emptyList())
        } else {
            //sidebarCategories变量处约定：
            //catId == null，全部
            //catId == -1L，未分类
            //catId >0，某个分类
            when (catId) {
                null -> favoriteRepo.observeFavorites(userId, null)
                -1L -> favoriteRepo.observeUncategorizedFavorites(userId)
                else -> favoriteRepo.observeFavorites(userId, catId)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    //点击收藏图标
    fun toggleFavorite(article: FavoriteArticleEntity) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            favoriteRepo.toggleFavorite(userId, article)
        }
    }

    //添加分类
    fun addCategory(name: String, onResult: (Result<Unit>) -> Unit) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            val result = favoriteRepo.addCategory(userId, name)
            onResult(result)
        }
    }

    //删除分类
    fun deleteCategory(categoryId: Long) {
        viewModelScope.launch { favoriteRepo.deleteCategory(categoryId) }
    }

    //将文章添加至分类
    fun setArticleCategory(articleId: Long, categoryId: Long?) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            favoriteRepo.setArticleCategory(userId, articleId, categoryId)
                .onSuccess {
                    ToastUtils.show("操作成功ヽ(✿ﾟ▽ﾟ)ノ")
                }
                .onFailure {
                    ToastUtils.show(it.message ?: "操作失败≧ ﹏ ≦")
                }
        }
    }

    //分类重命名
    fun renameCategory(categoryId: Long, newName: String, onResult: (Result<Unit>) -> Unit) {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            val result = favoriteRepo.renameCategory(userId, categoryId, newName)
            onResult(result)
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    //全量对账刷新
    fun refreshFavorites() {
        val userId = currentUserId.value ?: return
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                favoriteRepo.refreshAll(userId)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isRefreshing.value = false
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