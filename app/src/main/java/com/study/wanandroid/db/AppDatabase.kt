package com.study.wanandroid.db

import androidx.room.Database
import androidx.room.RoomDatabase
import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CategoryEntity::class,
        FavoriteArticleEntity::class
    ],
    // 数据库版本号。后续如果修改了 Entity 的字段，需要将其加 1，并编写 Migration
    version = 2,
    exportSchema = true// 配合在 build.gradle.kts 里配置的 schemaLocation，方便版本追踪
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun favoriteDao(): FavoriteDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        //初始化数据库
        fun init(context: Context) {
            if (INSTANCE == null) {
                INSTANCE = Room.databaseBuilder(
                    context = context.applicationContext,
                    klass = AppDatabase::class.java,
                    name = "wanandroid.db"//数据库名
                )
                    //开启外键约束(room默认开启)
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            db.execSQL("PRAGMA foreign_keys = ON")
                        }
                    })
                    //添加迁移策略
                    .addMigrations(Migration_1_2)
                    .build()
            }
        }

        //获取数据库实例
        fun getInstance(): AppDatabase {
            return INSTANCE ?: throw IllegalStateException(
                "AppDatabase尚未初始化"
            )
        }
    }
}