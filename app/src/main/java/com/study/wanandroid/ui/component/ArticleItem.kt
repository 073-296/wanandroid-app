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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.study.wanandroid.R
import com.study.wanandroid.common.customTab
import com.study.wanandroid.data.local.entity.FavoriteArticleEntity
import com.study.wanandroid.data.remote.dto.ArticleDetail
import com.study.wanandroid.ui.theme.WanAndroidTheme

@Composable
fun ArticleItem(
    modifier: Modifier = Modifier,
    articleDetail: ArticleDetail = ArticleDetail(),
    isCollected: Boolean,//当前文章是否已被收藏
    userId: Long,//当前登录用户 id（构造收藏实体用）
    onToggleFavorite: (FavoriteArticleEntity) -> Unit,//收藏状态切换回调
    highlightKeyword: String? = null//搜索页传入关键词，标题中命中的部分会被高亮
) {
    val context = LocalContext.current
    val highlightColor = MaterialTheme.colorScheme.primary
    //关键词不变时复用同一个 AnnotatedString，避免每次重组都重新扫描标题
    val titleText = remember(articleDetail.title, highlightKeyword, highlightColor) {
        buildHighlightedTitle(articleDetail.title, highlightKeyword, highlightColor)
    }
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
                    text = titleText,
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
                        userId = userId,
                        articleId = articleDetail.id.toLong(),
                        title = articleDetail.title,
                        author = articleDetail.author.ifBlank { articleDetail.shareUser },
                        link = articleDetail.link,
                        niceDate = articleDetail.niceDate
                    )
                    onToggleFavorite(entity)
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

/**
 * 把标题中命中关键词的片段染成主题色。
 *
 * 服务端搜索接口本来就用 <em class='highlight'> 标出了命中位置，但那份标记在数据层
 * 已经被清成纯文本了（收藏表存标题、分享标题都需要纯文本）。这里改成按关键词本地匹配，
 * 效果一致，而且不依赖服务端是否返回标记，大小写也能忽略。
 */
private fun buildHighlightedTitle(text: String, keyword: String?, color: Color): AnnotatedString {
    if (keyword.isNullOrBlank() || text.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        var cursor = 0
        while (cursor < text.length) {
            val hit = text.indexOf(keyword, cursor, ignoreCase = true)
            if (hit < 0) {
                append(text.substring(cursor))
                return@buildAnnotatedString
            }
            append(text.substring(cursor, hit))
            withStyle(SpanStyle(color = color, fontWeight = FontWeight.SemiBold)) {
                append(text.substring(hit, hit + keyword.length))
            }
            cursor = hit + keyword.length
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