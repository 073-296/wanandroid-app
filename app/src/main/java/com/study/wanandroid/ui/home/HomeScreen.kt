package com.study.wanandroid.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.study.wanandroid.ui.MainViewModel
import com.study.wanandroid.R
import com.study.wanandroid.data.remote.dto.ArticleDetail
import com.study.wanandroid.ui.component.ArticleItem
import com.study.wanandroid.ui.component.Banner
import com.study.wanandroid.ui.component.ContentBottomBar

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    currentRoute: String?,
    bottomBarOnClick: (route: String) -> Unit,
    searchOnClick: () -> Unit
) {
    Scaffold(
        modifier = Modifier.Companion
            .fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            HomeScreenTopBar(searchOnClick)
        },
        bottomBar = {
            ContentBottomBar(
                currentRoute = currentRoute,
                onClick = { route -> bottomBarOnClick(route) }
            )
        }
    ) { paddingValues ->
        HomeScreenContent(
            modifier = Modifier.Companion.padding(paddingValues),
            viewModel = viewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenTopBar(
    searchOnClick: () -> Unit
) {
    TopAppBar(
        title = {
            Text(text = stringResource(R.string.home))
        },
        actions = {
            IconButton(
                onClick = {
                    searchOnClick()
                }
            ) {
                Icon(
                    modifier = Modifier.Companion
                        .size(30.dp),
                    imageVector = Icons.Default.Search,
                    contentDescription = stringResource(R.string.search),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
private fun HomeScreenContent(modifier: Modifier, viewModel: MainViewModel) {
    val lazyPagingItems: LazyPagingItems<ArticleDetail> =
        viewModel.articles.collectAsLazyPagingItems()
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(start = 10.dp, top = 10.dp, end = 10.dp, bottom = 0.dp)
        ) {

            //轮播图
            item {
                Box(
                    modifier = Modifier.Companion
                        .height(180.dp)
                        .fillMaxWidth()
                ) {
                    Banner(viewModel)
                }
            }

            //文章列表
            items(
                count = lazyPagingItems.itemCount,
                key = lazyPagingItems.itemKey { it.id }
            ) { index ->
                val article = lazyPagingItems[index]
                article?.let { ArticleItem(articleDetail = it, viewModel = viewModel) }
            }

            //底部加载状态
            when (lazyPagingItems.loadState.append) {
                is LoadState.Loading -> {
                    item {
                        Box(
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Companion.Center
                        ) {
                            Text(text = "加载中...")
                        }
                    }
                }

                is LoadState.Error -> {
                    item {
                        Box(
                            modifier = Modifier.Companion
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Companion.Center
                        ) {
                            Text(
                                text = "加载失败，点击重试",
                                modifier = Modifier.Companion.clickable(onClick = { lazyPagingItems.retry() })
                            )
                        }
                    }
                }

                else -> Unit
            }
        }

        //首次加载/刷新状态
        when (lazyPagingItems.loadState.refresh) {
            is LoadState.Loading -> {
                Box(
                    modifier = Modifier.Companion
                        .fillMaxSize(),
                    contentAlignment = Alignment.Companion.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is LoadState.Error -> {
                Box(
                    modifier = Modifier.Companion
                        .fillMaxSize(),
                    contentAlignment = Alignment.Companion.Center
                ) {
                    Text(
                        text = "加载失败，点击重试",
                        modifier = Modifier.Companion.clickable(onClick = { lazyPagingItems.retry() })
                    )
                }
            }

            else -> Unit
        }
    }
}