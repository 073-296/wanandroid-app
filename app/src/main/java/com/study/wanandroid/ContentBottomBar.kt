package com.study.wanandroid

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.study.wanandroid.navigation.WanAndroidRoute
import com.study.wanandroid.ui.theme.WanAndroidTheme

//底部导航栏
@Composable
fun ContentBottomBar(
    currentRoute: String?,
    onClick: (route: String) -> Unit
) {
    val items = listOf(
        WanAndroidRoute.Home.name,
        WanAndroidRoute.Collect.name,
        WanAndroidRoute.Mine.name
    )
    NavigationBar {
        items.forEachIndexed { index, route ->
            NavigationBarItem(
                selected = currentRoute == route,
                onClick = {
                    onClick(route)
                },
                icon = {
                    when (index) {
                        0 -> Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = stringResource(R.string.home),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        1 -> Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(R.string.collect),
                            tint = MaterialTheme.colorScheme.primary
                        )

                        2 -> Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = stringResource(R.string.mine),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                label = {
                    when (index) {
                        0 -> Text(text = stringResource(R.string.home))
                        1 -> Text(text = stringResource(R.string.collect))
                        2 -> Text(text = stringResource(R.string.mine))
                    }
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ContentBottomBarPreview() {
    WanAndroidTheme {
        //ContentBottomBar(onClick = {})
    }
}