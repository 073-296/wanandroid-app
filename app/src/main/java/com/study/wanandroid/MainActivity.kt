package com.study.wanandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.study.wanandroid.navigation.MainNav
import com.study.wanandroid.ui.theme.WanAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedPreferences = getSharedPreferences(getString(R.string.sp_record), MODE_PRIVATE)
        val token = sharedPreferences.getString(getString(R.string.token).toString(), "")


        setContent {
            WanAndroidTheme {
                val viewModel: MainViewModel = viewModel()
                MainNav(viewModel = viewModel)
                //WanAndroidApp(token = token)
            }
        }
    }
}

//@Composable
//fun WanAndroidApp(token: String?) {
//    val context = LocalContext.current
//    MainNav()
//}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WanAndroidTheme {

    }
}