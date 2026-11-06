package com.study.wanandroid.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.study.wanandroid.R
import com.study.wanandroid.ui.AuthViewModel
import com.study.wanandroid.ui.collect.CollectScreen
import com.study.wanandroid.ui.collect.CollectViewModel
import com.study.wanandroid.ui.home.HomeScreen
import com.study.wanandroid.ui.home.HomeViewModel
import com.study.wanandroid.ui.login.LoginScreen
import com.study.wanandroid.ui.mine.MineScreen
import com.study.wanandroid.ui.search.SearchScreen
import com.study.wanandroid.ui.search.SearchViewModel


enum class WanAndroidRoute(@StringRes val title: Int) {
    Login(title = R.string.login),
    Home(title = R.string.home),
    Collect(title = R.string.collect),
    Mine(title = R.string.mine),
    Search(title = R.string.search)
}

@Composable
fun MainNav() {
    //在导航层统一创建各 ViewModel（此处作用域是 Activity，配置变更/页面切换时状态不丢）
    val authViewModel: AuthViewModel = viewModel()
    val homeViewModel: HomeViewModel = viewModel()
    val searchViewModel: SearchViewModel = viewModel()
    val collectViewModel: CollectViewModel = viewModel()

    val isLoggedIn by authViewModel.isLogged.collectAsState()

    val navController: NavHostController = rememberNavController()

    //使用 LaunchedEffect 监听登录状态变化，并伴随清空返回栈
    LaunchedEffect(isLoggedIn) {
        val targetRoute = if (isLoggedIn) WanAndroidRoute.Home.name else WanAndroidRoute.Login.name
        val currentRoute = navController.currentDestination?.route

        // 只有当前页面与目标页面不一致时才跳转，防止冷启动时多触发一次跳转
        if (currentRoute != null && currentRoute != targetRoute) {
            navController.navigate(targetRoute) {
                // 清空整个返回栈
                popUpTo(0) { inclusive = true }
            }
        }
    }

    //获取当前返回栈的栈顶条目，用它的 destination.route 判断当前页面
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    //底部导航跳转逻辑
    val navigateToBottomTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            //将Home作为默认起点的根
            popUpTo(WanAndroidRoute.Home.name) {
                saveState = true//弹出时保存被弹出页面的状态（如列表滚动位置）
            }
            //避免重复创建同一个目的地
            launchSingleTop = true
            //恢复之前保存的状态（滚动位置，输入内容等）
            restoreState = true
        }
    }

    NavHost(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        navController = navController,
        //初始占位，实际startDestination由LaunchedEffect块决定
        startDestination = if (isLoggedIn) WanAndroidRoute.Home.name else WanAndroidRoute.Login.name,
        enterTransition = { fadeIn(animationSpec = tween(300)) },
        exitTransition = { fadeOut(animationSpec = tween(300)) }
    ) {
        //登录注册
        composable(route = WanAndroidRoute.Login.name) {
            LoginScreen(viewModel = authViewModel)
        }

        //主页
        composable(route = WanAndroidRoute.Home.name) {
            HomeScreen(
                viewModel = homeViewModel,
                currentRoute = currentRoute,
                bottomBarOnClick = navigateToBottomTab,
                searchOnClick = {
                    navController.navigate(WanAndroidRoute.Search.name)
                }
            )
        }

        //收藏
        composable(route = WanAndroidRoute.Collect.name) {
            CollectScreen(
                viewModel = collectViewModel,
                currentRoute = currentRoute,
                bottomBarOnClick = navigateToBottomTab
            )
        }

        //我的
        composable(route = WanAndroidRoute.Mine.name) {
            MineScreen(
                viewModel = authViewModel,
                currentRoute = currentRoute,
                bottomBarOnClick = navigateToBottomTab
            )
        }

        //搜索
        composable(route = WanAndroidRoute.Search.name) {
            SearchScreen(
                viewModel = searchViewModel,
                backToHomeScreen = {
                    navController.popBackStack()
                }
            )
        }
    }
}