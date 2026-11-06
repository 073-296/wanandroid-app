package com.study.wanandroid.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

object DbConstants {
    // 表名
    const val TABLE_CATEGORIES = "categories"
    const val TABLE_FAVORITES = "favorite_articles"

    // 分类表字段
    const val COL_CAT_ID = "id"
    const val COL_CAT_USER_ID = "userId"
    const val COL_CAT_NAME = "name"
    const val COL_CAT_SORT_ORDER = "sortOrder"
    const val COL_CAT_CREATED_AT = "createdAt"

    // 收藏表字段
    const val COL_FAV_USER_ID = "userId"
    const val COL_FAV_ARTICLE_ID = "articleId"
    const val COL_FAV_TITLE = "title"
    const val COL_FAV_AUTHOR = "author"
    const val COL_FAV_LINK = "link"
    const val COL_FAV_NICE_DATE = "niceDate"
    const val COL_FAV_COLLECT_TIME = "collectTime"
    const val COL_FAV_SERVER_ORDER = "serverOrder"
    const val COL_FAV_PUBLISH_TIME = "publishTime"
    const val COL_FAV_CATEGORY_ID = "categoryId"
    const val COL_FAV_SYNC_STATE = "remoteSyncState"
    const val COL_FAV_LAST_ERROR = "lastError"
}

//本地分类表
@Entity(
    tableName = DbConstants.TABLE_CATEGORIES,
    //按用户查分类，并按 sortOrder 排序
    indices = [Index(DbConstants.COL_CAT_USER_ID, DbConstants.COL_CAT_SORT_ORDER)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    //记录这个分类属于哪个用户
    val userId: Long,
    val name: String,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

//收藏文章表
@Entity(
    tableName = DbConstants.TABLE_FAVORITES,
    //保证不同用户的同篇文章不会主键冲突，同一个用户下的一篇文章只有一条记录
    primaryKeys = [DbConstants.COL_FAV_USER_ID, DbConstants.COL_FAV_ARTICLE_ID],
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = [DbConstants.COL_CAT_ID],
            childColumns = [DbConstants.COL_FAV_CATEGORY_ID],
            onDelete = ForeignKey.Companion.SET_NULL
        )
    ],
    indices = [
        //查某个分类下的文章
        Index(DbConstants.COL_FAV_CATEGORY_ID),
        //查某个用户的所有待同步任务（Worker 扫表用）
        Index(DbConstants.COL_FAV_USER_ID, DbConstants.COL_FAV_SYNC_STATE),
        //查某个用户的某个分类下的文章（UI 筛选用）
        Index(DbConstants.COL_FAV_USER_ID, DbConstants.COL_FAV_CATEGORY_ID)
    ]
)
data class FavoriteArticleEntity(
    //标识与多账号隔离
    val userId: Long,
    val articleId: Long, // 对应 API 的 originId

    //UI 展示快照
    val title: String,
    val author: String? = null,
    val link: String,
    val niceDate: String? = null,

    //排序控制
    val collectTime: Long = 0L,
    val serverOrder: Int = -1,
    val publishTime: Long? = null,

    //本地分类与同步状态
    val categoryId: Long? = null,
    val remoteSyncState: RemoteSyncState = RemoteSyncState.SYNCED,
    val lastError: String? = null
)