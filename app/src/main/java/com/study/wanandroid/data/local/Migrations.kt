package com.study.wanandroid.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val Migration_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 1. 创建一张新表（去掉了 coverUrl 字段）
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `favorite_articles_new` (
                `userId` INTEGER NOT NULL,
                `articleId` INTEGER NOT NULL,
                `title` TEXT NOT NULL,
                `author` TEXT,
                `link` TEXT NOT NULL,
                `niceDate` TEXT,
                `collectTime` INTEGER NOT NULL,
                `serverOrder` INTEGER NOT NULL,
                `publishTime` INTEGER,
                `categoryId` INTEGER,
                `remoteSyncState` TEXT NOT NULL,
                `lastError` TEXT,
                PRIMARY KEY(`userId`, `articleId`),
                FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON UPDATE NO ACTION ON DELETE SET NULL
            )
        """.trimIndent()
        )

        // 2. 把旧表的数据拷过去（忽略 coverUrl）
        db.execSQL(
            """
            INSERT INTO `favorite_articles_new` (
                userId, articleId, title, author, link, niceDate, 
                collectTime, serverOrder, publishTime, categoryId, remoteSyncState, lastError
            )
            SELECT 
                userId, articleId, title, author, link, niceDate, 
                collectTime, serverOrder, publishTime, categoryId, remoteSyncState, lastError
            FROM `favorite_articles`
        """.trimIndent()
        )

        // 3. 删掉旧表
        db.execSQL("DROP TABLE `favorite_articles`")

        // 4. 新表改名
        db.execSQL("ALTER TABLE `favorite_articles_new` RENAME TO `favorite_articles`")

        // 5. 重新创建索引（因为表重建了，索引也丢了）
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_articles_categoryId` ON `favorite_articles` (`categoryId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_articles_userId_remoteSyncState` ON `favorite_articles` (`userId`, `remoteSyncState`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_favorite_articles_userId_categoryId` ON `favorite_articles` (`userId`, `categoryId`)")
    }
}