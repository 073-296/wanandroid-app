package com.study.wanandroid.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.study.wanandroid.ArticleItem
import com.study.wanandroid.Banner
import com.study.wanandroid.ContentBottomBar
import com.study.wanandroid.MainViewModel
import com.study.wanandroid.R
import com.study.wanandroid.entity.ArticleDetail

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    articleOnClick: (route: String) -> Unit,
    searchOnClick: () -> Unit
) {

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            HomeScreenTopBar(searchOnClick)
        },
        bottomBar = {
            HomeScreenBottomBar { route ->
                articleOnClick(route)
            }
        }
    ) { paddingValues ->
        HomeScreenContent(modifier = Modifier.padding(paddingValues), viewModel = viewModel)
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
            Icon(
                modifier = Modifier
                    .clickable(onClick = searchOnClick)
                    .size(30.dp),
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    )


}

@Composable
private fun HomeScreenBottomBar(onClick: (String) -> Unit) {
    ContentBottomBar { route ->
        onClick(route)
    }
}

@Composable
private fun HomeScreenContent(modifier: Modifier, viewModel: MainViewModel) {
    val lazyPagingItems: LazyPagingItems<ArticleDetail> =
        viewModel.articles.collectAsLazyPagingItems()
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp)
        ) {

            //轮播图
            item {
                Box(
                    modifier = Modifier
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
                article?.let { ArticleItem(articleDetail = it) }
            }

            //底部加载状态
            when (lazyPagingItems.loadState.append) {
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
                                modifier = Modifier.clickable(onClick = { lazyPagingItems.retry() })
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
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is LoadState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "加载失败，点击重试",
                        modifier = Modifier.clickable(onClick = { lazyPagingItems.retry() })
                    )
                }
            }

            else -> Unit
        }
    }
}



