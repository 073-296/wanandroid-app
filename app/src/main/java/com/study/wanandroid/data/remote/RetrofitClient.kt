package com.study.wanandroid.data.remote

import android.content.Context
import com.study.wanandroid.BuildConfig
import com.study.wanandroid.data.remote.PersistentCookieJar
import com.study.wanandroid.data.remote.SessionEvents
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://wanandroid.com/"

    //cookieJar
    private lateinit var cookieJar: PersistentCookieJar
    fun init(context: Context) {
        cookieJar = PersistentCookieJar(context = context)
    }

    fun getCookieJar(): PersistentCookieJar {
        return cookieJar
    }

    //日志拦截器
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = if (BuildConfig.DEBUG) {
            HttpLoggingInterceptor.Level.BODY
        } else {
            HttpLoggingInterceptor.Level.NONE
        }
    }

    //通用请求头拦截器
    private val headerInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            //.addHeader("Content-Type", "application/json")
            .addHeader("Accept", "application/json")
            .build()
        chain.proceed(request)
    }

    //全局鉴权拦截器
    private val authInterceptor = Interceptor { chain ->
        val response = chain.proceed(chain.request())
        //检查 HTTP 状态码 401
        var isUnauthorized = response.code == 401

        //使用 peekBody 克隆一份响应体，不会影响后续 Retrofit 的解析
        if (!isUnauthorized && response.isSuccessful) {
            try {
                val peekBody = response.peekBody(Long.MAX_VALUE).string()
                if (peekBody.contains("\"errorCode\":-1001") || peekBody.contains("\"errorCode\": -1001")) {
                    isUnauthorized = true
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        if (isUnauthorized) {
            SessionEvents.notifyUnauthorized()//发出全局踢出事件
        }

        response
    }

    //OkHttpClient
    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .cookieJar(cookieJar = cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    //Json
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true//服务端字段为 null 时自动用默认值兜底
        encodeDefaults = true//序列化默认值
    }

    //Retrofit
    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ApiService::class.java)
    }
}