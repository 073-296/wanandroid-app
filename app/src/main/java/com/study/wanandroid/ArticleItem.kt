package com.study.wanandroid

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.study.wanandroid.entity.ArticleDetail
import com.study.wanandroid.ui.theme.WanAndroidTheme

@Composable
fun ArticleItem(modifier: Modifier = Modifier, articleDetail: ArticleDetail = ArticleDetail()) {
    val context = LocalContext.current
    Card(
        modifier = modifier
            .padding(10.dp)
            .clickable { customTab(context, articleDetail.link) }
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
            ) {
                Text(
                    text = articleDetail.title,
                    fontSize = MaterialTheme.typography.titleLarge.fontSize,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                Text(
                    text = if (articleDetail.author != "") stringResource(
                        R.string.author,
                        articleDetail.author
                    ) else stringResource(R.string.share_user, articleDetail.shareUser),
                    fontSize = MaterialTheme.typography.labelLarge.fontSize,
                )
            }
            Spacer(
                modifier = Modifier
                    .size(15.dp)
                    .align(Alignment.CenterVertically)
            )
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = stringResource(R.string.collect),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(20.dp)
                    .clickable(onClick = {
                        //todo 收藏文章
                    })
            )
        }
    }
}

@Preview
@Composable
fun ArticleItemPreview() {
    WanAndroidTheme {
        ArticleItem()
    }
}