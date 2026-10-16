package com.study.wanandroid.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
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
    //获取当前返回栈的栈顶条目，用它的 destination.route 判断当前页面
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
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
                currentRoute = currentRoute,
                bottomBarOnClick = { route ->
                    navController.navigate(route) {
                        //弹出到起始目的地（Home），保存状态
                        popUpTo(WanAndroidRoute.Home.name) {
                            saveState = true//弹出时保存被弹出页面的状态（如列表滚动位置）
                        }
                        //避免重复创建同一个目的地
                        launchSingleTop = true
                        //恢复之前保存的状态（滚动位置，输入内容等）
                        restoreState = true
                    }
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