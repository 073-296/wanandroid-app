package com.study.wanandroid.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.study.wanandroid.ContentBottomBar
import com.study.wanandroid.MainViewModel
import com.study.wanandroid.ui.theme.WanAndroidTheme

@Composable
fun MineScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel,
    currentRoute: String?,
    bottomBarOnClick: (String) -> Unit
) {
    val userInfo by viewModel.userInfo.collectAsState()
    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            ContentBottomBar(
                currentRoute = currentRoute,
                onClick = { route -> bottomBarOnClick(route) }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(10.dp),
        ) {
            Row(
                modifier = Modifier,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    modifier = Modifier.size(70.dp),
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "用户",
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(
                    modifier = Modifier,
                ) {
                    Text(
                        text = userInfo?.username ?: "布吉岛"
                    )
                    Text(
                        text = "硬币：${userInfo?.coinCount ?: 0}"
                    )
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.logout() }
            ) {
                Text(
                    text = "退出登录"
                )
            }
        }
    }
}

@Preview
@Composable
fun MineScreenPreview() {
    WanAndroidTheme {
//        MineScreen(
//            currentRoute = null,
//            bottomBarOnClick = {}
//        )
    }
}