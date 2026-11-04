package com.study.wanandroid.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 用于接收带文章数量的分类查询结果
 */
data class CategoryWithCount(
    val id: Long,
    val userId: Long,
    val name: String,
    val sortOrder: Int,
    val createdAt: Long,
    val articleCount: Int // 聚合计算得出的文章数量
)

@Dao
interface CategoryDao {
    // 1. 观察分类列表（带文章数量统计）
    /**
     * 观察当前用户的所有分类，并统计每个分类下的文章数。
     * 注意：
     * 1. JOIN 条件里必须包含 f.userId = :userId，防止统计到其他用户的文章。
     * 2. 必须排除 remoteSyncState = 'PENDING_DELETE' 的文章，因为用户虽然没网，但已经点了取消收藏。
     */
    @Query("""
        SELECT c.*, COUNT(f.${DbConstants.COL_FAV_ARTICLE_ID}) AS articleCount
        FROM ${DbConstants.TABLE_CATEGORIES} c
        LEFT JOIN ${DbConstants.TABLE_FAVORITES} f 
            ON c.${DbConstants.COL_CAT_ID} = f.${DbConstants.COL_FAV_CATEGORY_ID} 
            AND f.${DbConstants.COL_FAV_USER_ID} = :userId
            AND f.${DbConstants.COL_FAV_SYNC_STATE} != 'PENDING_DELETE'
        WHERE c.${DbConstants.COL_CAT_USER_ID} = :userId
        GROUP BY c.${DbConstants.COL_CAT_ID}
        ORDER BY c.${DbConstants.COL_CAT_SORT_ORDER} ASC, c.${DbConstants.COL_CAT_CREATED_AT} ASC
    """)
    fun observeCategoriesWithCount(userId: Long): Flow<List<CategoryWithCount>>

    //2. 基础 CRUD 操作
    /**
     * 插入新分类，返回自增的 ID。
     */
    @Insert
    suspend fun insert(category: CategoryEntity): Long

    /**
     * 更新分类（重命名、修改 sortOrder）。
     */
    @Update
    suspend fun update(category: CategoryEntity)

    /**
     * 批量更新分类（用于用户拖拽排序后，一次性更新所有发生改变的分类）。
     */
    @Update
    suspend fun updateAll(categories: List<CategoryEntity>)

    /**
     * 删除分类。
     * 核心：因为在 Entity 里配置了外键 onDelete = SET_NULL，
     * 所以这里只要执行 delete，属于该分类的所有收藏文章的 categoryId 都会被 SQLite 自动置为 NULL。
     */
    @Query("DELETE FROM ${DbConstants.TABLE_CATEGORIES} WHERE ${DbConstants.COL_CAT_ID} = :categoryId")
    suspend fun deleteById(categoryId: Long)

    //3. 校验与辅助查询
    /**
     * 查询单个分类（用于设置文章分类时，校验分类是否属于当前用户）。
     */
    @Query("""
        SELECT * FROM ${DbConstants.TABLE_CATEGORIES} 
        WHERE ${DbConstants.COL_CAT_ID} = :categoryId 
          AND ${DbConstants.COL_CAT_USER_ID} = :userId
    """)
    suspend fun getCategoryById(categoryId: Long, userId: Long): CategoryEntity?

    /**
     * 获取该用户当前最大的 sortOrder 值。
     * 当用户新建分类时，用这个值 +1 作为新分类的排序位置，确保排在最后面。
     */
    @Query("""
        SELECT MAX(${DbConstants.COL_CAT_SORT_ORDER}) 
        FROM ${DbConstants.TABLE_CATEGORIES} 
        WHERE ${DbConstants.COL_CAT_USER_ID} = :userId
    """)
    suspend fun getMaxSortOrder(userId: Long): Int?

    /**
     * 检查同一用户下，分类名称是否已经存在（防重名）。
     * 注意：更新名称时，需要排除自己当前的 ID。
     */
    @Query("""
        SELECT COUNT(*) FROM ${DbConstants.TABLE_CATEGORIES} 
        WHERE ${DbConstants.COL_CAT_USER_ID} = :userId 
          AND ${DbConstants.COL_CAT_NAME} = :name 
          AND ${DbConstants.COL_CAT_ID} != :excludeCategoryId
    """)
    suspend fun checkNameExists(userId: Long, name: String, excludeCategoryId: Long = -1L): Int

    // 4. 账号数据清理
    /**
     * 清空某个用户的**所有**分类。
     * 使用场景：用户点击“清空分类”功能，或者长时间不用的过期账号清理本地数据。
     * （注：执行后，该用户所有文章的 categoryId 会因外键约束自动变为 null）。
     */
    @Query("DELETE FROM ${DbConstants.TABLE_CATEGORIES} WHERE ${DbConstants.COL_CAT_USER_ID} = :userId")
    suspend fun deleteAllByUserId(userId: Long)
}