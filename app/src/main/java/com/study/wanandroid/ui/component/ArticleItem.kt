package com.study.wanandroid.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.study.wanandroid.ui.MainViewModel
import com.study.wanandroid.R
import com.study.wanandroid.common.customTab
import com.study.wanandroid.data.local.entity.FavoriteArticleEntity
import com.study.wanandroid.data.remote.dto.ArticleDetail
import com.study.wanandroid.ui.theme.WanAndroidTheme

@Composable
fun ArticleItem(
    modifier: Modifier = Modifier.Companion,
    articleDetail: ArticleDetail = ArticleDetail(),
    viewModel: MainViewModel
) {
    val context = LocalContext.current
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val isCollected: Boolean = articleDetail.id.toLong() in favoriteIds
    Card(
        modifier = modifier
            .padding(10.dp)
            .heightIn(min = 80.dp)
            .clickable { customTab(context, articleDetail.link) }
    ) {
        Row(
            modifier = Modifier.Companion
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.Companion.CenterVertically
        ) {
            Column(
                modifier = Modifier.Companion
                    .weight(1f)
                    .padding(1.dp)
            ) {
                Text(
                    text = articleDetail.title,
                    style = MaterialTheme.typography.titleMedium,
                    overflow = TextOverflow.Companion.Ellipsis,
                    maxLines = 2,
                    minLines = 2
                )
                Spacer(modifier = Modifier.Companion.height(4.dp))
                Text(
                    text = if (articleDetail.author.isNotBlank()) stringResource(
                        R.string.author,
                        articleDetail.author
                    ) else stringResource(R.string.share_user, articleDetail.shareUser),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Companion.Ellipsis,
                )
            }
            IconButton(
                onClick = {
                    val entity = FavoriteArticleEntity(
                        userId = viewModel.currentUserId.value ?: 0L,
                        articleId = articleDetail.id.toLong(),
                        title = articleDetail.title,
                        author = articleDetail.author.ifBlank { articleDetail.shareUser },
                        link = articleDetail.link,
                        niceDate = articleDetail.niceDate
                    )
                    viewModel.toggleFavorite(entity)
                }
            ) {
                Icon(
                    imageVector = Icons.Filled.Favorite,
                    contentDescription = stringResource(R.string.collect),
                    tint = if (isCollected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.Companion
                        .align(Alignment.Companion.CenterVertically)
                        .size(30.dp)
                )
            }
        }

    }
}

@Preview
@Composable
fun ArticleItemPreview() {
    WanAndroidTheme {
        //ArticleItem()
    }
}