package com.study.wanandroid

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.study.wanandroid.entity.BannerImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

//轮播图
@Composable
fun Banner(viewModel: MainViewModel) {
    val bannerState = viewModel.bannerState.collectAsState()
//    Box(
//        modifier = Modifier
//            .fillMaxSize()
//    ) {
    when (bannerState.value) {
        is BannerState.Loading -> {}
        is BannerState.Success -> {
            val images = (bannerState.value as BannerState.Success).banner
            BannerHorizontalPager(images = images)
        }

        is BannerState.Error -> {}
    }
    //}
}

@Composable
fun BannerHorizontalPager(images: List<BannerImage>, autoScrollDuration: Long = 3000L) {
    val pagerCount = images.size
    val pagerState = rememberPagerState {
        pagerCount
    }
    Box(
        contentAlignment = Alignment.BottomCenter
    ) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 10.dp),
            pageSpacing = 16.dp
        ) { index ->
            BannerCard(
                bannerImage = images[index],
                modifier = Modifier.padding(10.dp)
            )
        }
        DotIndicators(pageCount = pagerCount, pagerState = pagerState)
    }

    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()
    if (isDragged.not()) {
        with(pagerState) {
            var currentPageKey by remember { mutableIntStateOf(0) }
            LaunchedEffect(key1 = currentPageKey) {
                launch {
                    delay(timeMillis = autoScrollDuration)
                    val nextPage = (currentPage + 1).mod(pagerCount)
                    animateScrollToPage(page = nextPage)
                    currentPageKey = nextPage
                }
            }
        }
    }

}

@Composable
fun BannerCard(bannerImage: BannerImage, modifier: Modifier = Modifier) {
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
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
fun DotIndicators(
    pageCount: Int,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    Row(
        //modifier = modifier.padding(bottom = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom
    ) {
        repeat(pageCount) { iteration ->
            val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurface

            Canvas(
                modifier = Modifier
                    .size(12.dp)
                    .padding(horizontal = 2.dp),
                onDraw = {
                    drawCircle(color = color)
                }
            )
        }
    }
}