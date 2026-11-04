package com.study.wanandroid.db

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.study.wanandroid.network.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

class FavoriteSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        //1.获取传递进来的参数
        val userId = inputData.getLong("userId", -1L)
        val articleId = inputData.getLong("articleId", -1L)
        val isAdd = inputData.getBoolean("isAdd", false)

        //参数校验，如果不合法直接失败
        if (userId <= 0 || articleId <= 0) {
            return@withContext Result.failure()
        }

        val dao = AppDatabase.getInstance().favoriteDao()
        val api = RetrofitClient.apiService

        //2.获取本地记录，确认状态是否还需要同步
        val local = dao.getFavorite(userId, articleId)

        // 防护机制：如果在排队期间，用户切换了账号，或者记录已经被删了，直接结束
        // 注：退出账号时会通过 tag 取消 Worker，这里是双重保险
        if (local == null) return@withContext Result.success()

        try {
            if (isAdd) {
                //处理添加收藏
                if (local.remoteSyncState != RemoteSyncState.PENDING_ADD) {
                    return@withContext Result.success()
                }

                val response = api.addFavorite(articleId)
                if (response.errorCode == 0) {
                    //请求成功，更新状态为已同步
                    dao.markAsSynced(userId, articleId)
                    return@withContext Result.success()
                } else if (response.errorCode == -1001) {
                    //业务层未登录（兜底）
                    dao.setLastError(userId, articleId, response.errorMsg)
                    return@withContext Result.failure()
                } else {
                    //其他业务错误，记录并重试
                    dao.setLastError(userId, articleId, response.errorMsg)
                    return@withContext Result.retry()
                }

            } else {
                //处理取消收藏
                if (local.remoteSyncState != RemoteSyncState.PENDING_DELETE) {
                    return@withContext Result.success()
                }

                val response = api.removeFavorite(articleId)
                if (response.errorCode == 0) {
                    //请求成功，物理删除本地记录
                    dao.deleteById(userId, articleId)
                    return@withContext Result.success()
                } else if (response.errorCode == -1001) {
                    dao.setLastError(userId, articleId, response.errorMsg)
                    return@withContext Result.failure()
                } else {
                    dao.setLastError(userId, articleId, response.errorMsg)
                    return@withContext Result.retry()
                }
            }
        } catch (e: Exception) {
            when (e) {
                is HttpException -> {
                    when (e.code()) {
                        401 -> {
                            // Cookie 彻底失效。不重试，交给全局拦截器处理
                            dao.setLastError(userId, articleId, "未授权，请重新登录")
                            Result.failure()
                        }

                        404 -> {
                            // 如果取消收藏时返回 404，说明服务端早就没这个数据了，视为成功
                            if (!isAdd) {
                                dao.deleteById(userId, articleId)
                                Result.success()
                            } else {
                                Result.failure()
                            }
                        }

                        else -> {
                            dao.setLastError(userId, articleId, "HTTP错误: ${e.code()}")
                            Result.retry()
                        }
                    }
                }

                is IOException -> {
                    // 断网或超时，记录错误并重试（WorkManager 自带指数退避）
                    dao.setLastError(userId, articleId, "网络连接失败")
                    Result.retry()
                }

                else -> {
                    dao.setLastError(userId, articleId, "未知错误: ${e.message}")
                    Result.failure()
                }
            }
        }
    }
}