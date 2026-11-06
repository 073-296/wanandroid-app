package com.study.wanandroid.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.study.wanandroid.ui.BannerState
import com.study.wanandroid.ui.MainViewModel
import com.study.wanandroid.common.customTab
import com.study.wanandroid.data.remote.dto.BannerImage
import kotlinx.coroutines.delay

//轮播图
@Composable
fun Banner(viewModel: MainViewModel) {
    val bannerState by viewModel.bannerState.collectAsState()
    when (val state = bannerState) {
        is BannerState.Loading -> {

        }

        is BannerState.Success -> {
            if (state.banner.isNotEmpty()) {
                BannerHorizontalPager(images = state.banner)
            }
        }

        is BannerState.Error -> {
            Box(
                modifier = Modifier.Companion
                    .fillMaxSize()
                    .clickable { viewModel.retryBanner() },
                contentAlignment = Alignment.Companion.Center
            ) {
                Text(
                    text = "加载失败，点击重试",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun BannerHorizontalPager(images: List<BannerImage>, autoScrollDuration: Long = 3000L) {
    val actualCount = images.size
    if (actualCount == 0) return

    //无限轮播核心：一个极大的虚拟页数
    val virtualCount = Int.MAX_VALUE
    //初始页面设置在中间，且取模后正好对应真实的第 0 张图
    val initialPage = virtualCount / 2 - (virtualCount / 2) % actualCount
    val pagerState = rememberPagerState(initialPage = initialPage) { virtualCount }
    val lifecycleOwner = LocalLifecycleOwner.current
    val isResumed by lifecycleOwner.lifecycle.currentStateAsState()
    Box(
        modifier = Modifier.Companion.fillMaxSize(),
        contentAlignment = Alignment.Companion.BottomCenter
    ) {
        HorizontalPager(
            modifier = Modifier.Companion.fillMaxSize(),
            state = pagerState,
            //contentPadding = PaddingValues(horizontal = 10.dp),
            //pageSpacing = 16.dp
        ) { page ->
            val realIndex = page % actualCount
            BannerCard(
                bannerImage = images[realIndex],
                modifier = Modifier.Companion.fillMaxSize()
            )
        }
        DotIndicators(
            pageCount = actualCount,
            currentPage = pagerState.currentPage % actualCount,
            modifier = Modifier.Companion.padding(bottom = 10.dp)
        )
    }

    //用户滑动/自动轮播
    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(
        key1 = isDragged,//boolean,从一个布尔值变为另一个布尔值时重启
        key2 = isResumed
    ) {
        if (!isDragged && isResumed == Lifecycle.State.RESUMED) {
            while (true) {
                delay(timeMillis = autoScrollDuration)
                //virtualCount足够大，可以直接+1
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }

}

@Composable
fun BannerCard(bannerImage: BannerImage, modifier: Modifier = Modifier.Companion) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = {
                customTab(context = context, url = bannerImage.url)
            })
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)//ImageRequest 本身
                .data(bannerImage.imagePath)
                .crossfade(true)//在请求成功完成时启用淡入淡出动画
                .build(),
            contentDescription = bannerImage.desc,
            contentScale = ContentScale.Companion.Crop,//Crop 保持比例并填满容器
            modifier = Modifier.Companion.fillMaxSize()
        )
    }
}

@Composable
fun DotIndicators(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier.Companion
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Companion.CenterVertically
    ) {
        repeat(pageCount) { iteration ->
            val color = if (currentPage == iteration) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

            Canvas(
                modifier = Modifier.Companion
                    .size(12.dp)
                    .padding(horizontal = 2.dp),
                onDraw = {
                    drawCircle(color = color)
                }
            )
        }
    }
}