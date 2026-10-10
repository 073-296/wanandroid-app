package com.study.wanandroid.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ModifierInfo
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.study.wanandroid.ui.theme.WanAndroidTheme

@Composable
fun LoginScreen(modifier: Modifier = Modifier) {
    var account by remember { mutableStateOf("") }
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
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun LoginScreenPreview() {
    WanAndroidTheme {
        LoginScreen()
    }
}