package com.study.wanandroid.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.study.wanandroid.MainViewModel
import com.study.wanandroid.R
import com.study.wanandroid.screen.HomeScreen
import com.study.wanandroid.screen.LoginScreen
import com.study.wanandroid.screen.MineScreen
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
    //isLogin: Boolean,
    viewModel: MainViewModel,
    //navController: NavHostController = rememberNavController()
) {
    val isLoggedIn by viewModel.isLogged.collectAsState()

    key(isLoggedIn) {
        val navController: NavHostController = rememberNavController()

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
            navController = navController,
            //初始占位，实际startDestination由LaunchedEffect块决定
            startDestination = if (isLoggedIn) WanAndroidRoute.Home.name else WanAndroidRoute.Login.name
        ) {
            //登录注册
            composable(route = WanAndroidRoute.Login.name) {
                LoginScreen(viewModel = viewModel)
            }

            //主页
            composable(route = WanAndroidRoute.Home.name) {
                HomeScreen(
                    viewModel = viewModel,
                    currentRoute = currentRoute,
                    bottomBarOnClick = navigateToBottomTab,
                    searchOnClick = {
                        navController.navigate(WanAndroidRoute.Search.name)
                    }
                )
            }

            //收藏
            composable(route = WanAndroidRoute.Collect.name) {

            }

            //我的
            composable(route = WanAndroidRoute.Mine.name) {
                MineScreen(
                    //viewModel = viewModel,
                    currentRoute = currentRoute,
                    bottomBarOnClick = navigateToBottomTab
                )
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

    //当登录状态变化时，自动切换页面
//    LaunchedEffect(isLoggedIn) {
//        val targetRoute = if (isLoggedIn) WanAndroidRoute.Home.name else WanAndroidRoute.Login.name
//        navController.navigate(targetRoute) {
//            // 清空整个返回栈，防止按返回键回到登录页或主页
//            popUpTo(0) { inclusive = true }
//        }
//    }

    //todo:如果说currentRoute表示的当前显示的界面的路由，当前代码逻辑为
    //从MainNav获得当前界面路由传给界面
    //界面将路由传给ContentBottomBar,ContentBottomBar只用它来判断哪个图标被选中
    //接着ContentBottomBar通过onClick将它里面 自带(items列表变量) 的三个底部图标中被选中的那个图标的路由传到界面
    //界面又通过bottomBarOnClick将路由传回到这里

    //所以，路由被传回到这里的那些步骤是不是没必要？
    //我只需要传回一个那个底部导航栏图标被点击时的动作（不带有参数的函数类型）就行了呢

    //todo:当前实现的HomeScreen和MineScreen之间进行切换时有时要点两次才跳转，而且出现了界面被多次创建的问题



}