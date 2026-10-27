package com.study.wanandroid.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File

class PersistentCookieJar(context: Context) : CookieJar {
    private val cookieFile = File(context.filesDir, "cookies.txt")

    //内存缓存+文件持久化
    //键：域名；值：cookie列表
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    init {
        loadFromFile()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        //遍历所有域名
        return cookieStore.values.flatten().filter {
            it.expiresAt > now && it.matches(url)
        }
    }

    @Synchronized
    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>
    ) {
        if (cookies.isEmpty()) return

        val currentCookies = cookieStore.getOrPut(url.host) { mutableListOf() }

        // 合并逻辑：根据 name 和 path 判断，去除旧的同名 Cookie，添加新的同名Cookie
        for (newCookie in cookies) {
            val iterator = currentCookies.iterator()
            while (iterator.hasNext()) {
                val oldCookie = iterator.next()
                if (oldCookie.name == newCookie.name && oldCookie.path == newCookie.path) {
                    iterator.remove() // 移除旧的同名 Cookie
                }
            }
            // 如果新 Cookie 还没过期，就加进去
            if (newCookie.expiresAt > System.currentTimeMillis()) {
                currentCookies.add(newCookie)
            }
        }
        saveToFile()
    }

    //文件读写
    private fun saveToFile() {
        //临时文件：处理数据写了但没写完报错的情况
        val tempFile = File(cookieFile.parentFile, "cookies.tmp")
        try {
            //往临时文件里写
            tempFile.bufferedWriter().use { writer ->
                for (cookies in cookieStore.values) {
                    for (cookie in cookies) {
                        // 使用 toString() 序列化为标准 HTTP Cookie 头格式
                        writer.write(cookie.toString())
                        writer.newLine()
                    }
                }
            }

            // 如果代码能走到这里，说明临时文件写完了，没出任何异常
            // 现在开始替换：删掉旧的，改名新的
            if (cookieFile.exists()) {
                cookieFile.delete()
            }
            tempFile.renameTo(cookieFile)

        } catch (e: Exception) {
            e.printStackTrace()
            //如果写临时文件报错了，直接删掉残缺的临时文件，正式文件（旧数据）原封不动
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun loadFromFile() {
        if (!cookieFile.exists()) return

        try {
            // 提取到循环外面，只解析一次
            val dummyUrl = "https://wanandroid.com/".toHttpUrl()
            cookieFile.bufferedReader().useLines { lines ->
                lines.forEach { line ->
                    // 使用 Cookie.parse 还原
                    Cookie.parse(dummyUrl, line)?.let { cookie ->
                        val host = cookie.domain // 或者直接用固定 url.host
                        val currentCookies = cookieStore.getOrPut(host) { mutableListOf() }
                        // 避免重复加载
                        currentCookies.removeAll { it.name == cookie.name && it.path == cookie.path }
                        currentCookies.add(cookie)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //判断本地是否持有有效的cookie
    @Synchronized
    fun hasAuthCookie(): Boolean {
        val now = System.currentTimeMillis()
        return cookieStore.values.flatten().any {
            it.expiresAt > now && (it.name == "token_pass" || it.name == "loginUserName")
        }
    }

    //清除Cookie(退出登录时使用)
    @Synchronized
    fun clear() {
        cookieStore.clear()
        if (cookieFile.exists()) cookieFile.delete()
    }
}