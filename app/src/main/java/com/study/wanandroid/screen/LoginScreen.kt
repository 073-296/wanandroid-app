package com.study.wanandroid.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.study.wanandroid.AuthState
import com.study.wanandroid.MainViewModel
import com.study.wanandroid.R
import com.study.wanandroid.ToastUtils
import com.study.wanandroid.ui.theme.WanAndroidTheme
import kotlinx.coroutines.launch

private enum class AuthTab(@StringRes val titleRes: Int) {
    LOGIN(R.string.login),
    REGISTER(R.string.register)
}

@Composable
fun LoginScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    Scaffold(
        modifier = modifier
    ) { paddingValues ->


        CustomSwitchingPage(
            modifier = Modifier.padding(paddingValues),
            viewModel = viewModel
        )
    }
    /*    var account by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var isRegister by remember { mutableStateOf(false) }
        var rePassword by remember { mutableStateOf("") }
        Column(
            modifier = modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(10.dp)
        ) {
            OutlinedTextField(
                value = account,
                onValueChange = { account = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = "account")
                },
                placeholder = { Text(text = "请输入账号") }
            )

            Spacer(modifier = Modifier.padding(5.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(text = "password")
                },
                placeholder = { Text(text = "请输入密码") }
            )

            Spacer(modifier = Modifier.padding(5.dp))

            if(isRegister){
                OutlinedTextField(
                    value = rePassword,
                    onValueChange = { rePassword = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(text = "rePassword")
                    },
                    placeholder = { Text(text = "请再次输入密码") }
                )
            }

            Spacer(modifier = Modifier.padding(5.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(modifier= Modifier, onClick = {}){
                    Text(text = "登录")
                }

                Button(modifier= Modifier, onClick = {isRegister=true}){
                    Text(text = "注册")
                }
            }
        }*/


}

@Composable
fun CustomSwitchingPage(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel
) {
    val tabs = AuthTab.entries
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { tabs.size })
    val scope = rememberCoroutineScope()

    //监听认证状态
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    //抽出"切页"动作,tab点击和页面内按钮复用
    val switchTo: (AuthTab) -> Unit = { tab ->
        scope.launch { pagerState.animateScrollToPage(tab.ordinal) }
    }

    //todo:登录成功和退出成功的json体一样的，都进入到AuthState.Success这条分支了
    LaunchedEffect(authState) {
        when (val state=authState) {
            is AuthState.Loading -> {
                //ToastUtils.show("请稍后")
            }

            is AuthState.Success -> {
                //Toast.makeText(context,"登录成功", Toast.LENGTH_SHORT).show()
//                if (state.userInfo != null) {
//                    ToastUtils.show("登录成功")
//                } else {
//                    ToastUtils.show("退出成功")
//                }
            }

            is AuthState.RegisterSuccess -> {
                //Toast.makeText(context,"注册成功，请登录", Toast.LENGTH_SHORT).show()
                ToastUtils.show("注册成功，请登录")
                pagerState.animateScrollToPage(0)//切回登录页
            }

            is AuthState.Error -> {
                //Toast.makeText(context,(authState as AuthState.Error).message, Toast.LENGTH_SHORT).show()
                ToastUtils.show((authState as AuthState.Error).message)
            }

            else -> Unit
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        TabRow(
            selectedTabIndex = pagerState.currentPage
        ) {
            tabs.forEach { tab ->
                Tab(
                    selected = pagerState.currentPage == tab.ordinal,
                    onClick = { switchTo(tab) },
                    text = { Text(text = stringResource(tab.titleRes)) }
                )
            }
        }

        HorizontalPager(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            state = pagerState,
        ) { page ->
            when (tabs[page]) {
                AuthTab.LOGIN -> {
                    LoginContent(
                        onLoginClick = { account, password ->
                            viewModel.login(account, password)
                        },
                        onGoRegister = { switchTo(AuthTab.REGISTER) }
                    )
                }

                AuthTab.REGISTER -> {
                    RegisterContent(
                        onRegisterClick = { account, password, rePassword ->
                            viewModel.register(account, password, rePassword)
                        },
                        onGoLogin = { switchTo(AuthTab.LOGIN) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LoginContent(
    modifier: Modifier = Modifier,
    onLoginClick: (account: String, password: String) -> Unit = { _, _ -> },
    onGoRegister: () -> Unit = {}
) {
    //rememberSaveable：当切页/旋转屏幕时输入的数据不丢失
    var account by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = account,
                onValueChange = { account = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("账号") },
                placeholder = { Text("请输入账号") },
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                placeholder = { Text("请输入密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onLoginClick(account, password) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.login))
            }

            TextButton(onClick = onGoRegister) {
                Text("没有账号？去注册")
            }
        }
    }
}

@Composable
private fun RegisterContent(
    modifier: Modifier = Modifier,
    onRegisterClick: (account: String, password: String, rePassword: String) -> Unit = { _, _, _ -> },
    onGoLogin: () -> Unit = {}
) {
    var account by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var rePassword by rememberSaveable { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                value = account,
                onValueChange = { account = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("账号") },
                placeholder = { Text("请设置账号") },
                singleLine = true,
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("密码") },
                placeholder = { Text("请设置密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            )

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = rePassword,
                onValueChange = { rePassword = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("确认密码") },
                placeholder = { Text("请再次输入密码") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = rePassword.isNotEmpty() && rePassword != password,
                supportingText = {
                    if (rePassword.isNotEmpty() && rePassword != password) {
                        Text("两次输入的密码不一致")
                    }
                },
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = { onRegisterClick(account, password, rePassword) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.register))
            }

            TextButton(onClick = onGoLogin) {
                Text("已有账号？去登录")
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    WanAndroidTheme {
        //LoginScreen()
    }
}