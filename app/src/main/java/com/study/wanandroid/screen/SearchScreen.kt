package com.study.wanandroid.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.study.wanandroid.ArticleItem
import com.study.wanandroid.MainViewModel
import com.study.wanandroid.R
import com.study.wanandroid.ui.theme.Shapes
import com.study.wanandroid.ui.theme.WanAndroidTheme

@Composable
fun SearchScreen(
    modifier: Modifier = Modifier,
    backToHomeScreen: () -> Unit = {},
    viewModel: MainViewModel
) {
    val searchResults = viewModel.searchResults.collectAsLazyPagingItems()
    val isSearchActive by viewModel.isSearchActive.collectAsState()
    var inputText by remember { mutableStateOf("") }

    //控制键盘收起
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    //用户手势返回HomeScreen时，清空搜索记录
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSearch()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 20.dp, end = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.clearSearch()
                        backToHomeScreen()
                    }
                ) {
                    Icon(
                        modifier = Modifier.size(30.dp),
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                OutlinedTextField(
                    modifier = Modifier.weight(1f),
                    value = inputText,
                    onValueChange = {
                        inputText = it
                    },
                    placeholder = { Text(text = "热搜词") },
                    shape = Shapes.small,
                    singleLine = true,
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                                viewModel.search(inputText)
                            }
                        ) {
                            Icon(
                                modifier = Modifier
                                    .size(30.dp),
                                imageVector = Icons.Default.Search,
                                contentDescription = stringResource(R.string.search),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            viewModel.search(inputText)
                        }
                    )
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                ) {
                    items(
                        count = searchResults.itemCount,
                        key = searchResults.itemKey { it.id }
                    ) { index ->
                        val article = searchResults[index]
                        article?.let { ArticleItem(articleDetail = it) }
                    }

                    //底部加载状态
                    when (searchResults.loadState.append) {
                        is LoadState.Loading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = "加载中...")
                                }
                            }
                        }

                        is LoadState.Error -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "加载失败，点击重试",
                                        modifier = Modifier.clickable(onClick = { searchResults.retry() })
                                    )
                                }
                            }
                        }

                        else -> Unit
                    }

                    //到底提示
                    if (searchResults.loadState.append.endOfPaginationReached
                        && searchResults.itemCount > 0
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "-已经到底啦-",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                //加载状态处理
                if (isSearchActive && searchResults.loadState.refresh is LoadState.Loading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }


                if (searchResults.loadState.refresh is LoadState.Error) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "加载失败，点击重试",
                            modifier = Modifier.clickable { searchResults.retry() }
                        )
                    }
                }
            }

        }
    }


}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SearchScreenPreview() {
    WanAndroidTheme {
        //SearchScreen()
    }
}
