package com.study.wanandroid.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.study.wanandroid.MainViewModel
import com.study.wanandroid.screen.HomeScreen
import com.study.wanandroid.screen.LoginScreen
import com.study.wanandroid.R
import com.study.wanandroid.screen.SearchScreen


enum class WanAndroidRoute(@StringRes val title: Int) {
    Login(title = R.string.login),
    Home(title = R.string.home),
    Collect(title = R.string.collect),
    Mine(title = R.string.mine),
    Search(title = R.string.search)
}

@Composable
fun MainNav(
    isLogin: Boolean,
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel()
) {
    //返回堆栈的引用
    val backStackEntry by navController.currentBackStackEntryAsState()
    NavHost(
        navController = navController,
        startDestination = if (isLogin) WanAndroidRoute.Home.name else WanAndroidRoute.Login.name
    ) {
        //登录注册
        composable(route = WanAndroidRoute.Login.name) {
            LoginScreen()
        }

        //主页
        composable(route = WanAndroidRoute.Home.name) {
            HomeScreen(
                viewModel = viewModel,
                articleOnClick = { route ->
                    navController.navigate(route)
                },
                searchOnClick = {
                    navController.navigate(WanAndroidRoute.Search.name)
                })
        }

        //收藏
        composable(route = WanAndroidRoute.Collect.name) {

        }

        //我的
        composable(route = WanAndroidRoute.Mine.name) {

        }

        //搜索
        composable(route = WanAndroidRoute.Search.name) {
            SearchScreen(
                viewModel = viewModel,
                backToHomeScreen = {
                    navController.popBackStack()
                }
            )
        }
    }
}