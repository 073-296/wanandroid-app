package com.study.wanandroid.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class PersistentCookieJar(context: Context) : CookieJar {
    private val cookieFile = File(context.filesDir, "cookies.txt")

    //内存缓存+文件持久化
    private val cookieStore = mutableMapOf<String, MutableList<Cookie>>()

    init {
        loadFromFile()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        //return cookieStore[url.host] ?: emptyList()

        val cookies = cookieStore[url.host] ?: return emptyList()
        val now = System.currentTimeMillis()

        // 过滤掉已过期的 Cookie，并剔除已经不符合当前请求的 Cookie
        val validCookies = cookies.filter {
            it.expiresAt > now && it.matches(url)
        }

        // 如果发现有过滤掉的 Cookie，顺便保存一下清理后的结果
        if (validCookies.size != cookies.size) {
            cookieStore[url.host] = validCookies.toMutableList()
            saveToFile()
        }
        return validCookies
    }

    @Synchronized
    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>
    ) {
        //cookieStore[url.host] = cookies
        //saveToFile()
        if (cookies.isEmpty()) return

        val currentCookies = cookieStore.getOrPut(url.host) { mutableListOf() }

        // 正确的合并逻辑：根据 name 和 path 覆盖同名 Cookie，而不是整体覆盖
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
//        try {
//            ObjectOutputStream(FileOutputStream(cookieFile)).use { oos ->
//                oos.writeObject(HashMap(cookieStore))
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }

        try {
            cookieFile.bufferedWriter().use { writer ->
                for (cookies in cookieStore.values) {
                    for (cookie in cookies) {
                        // 使用 toString() 序列化为标准 HTTP Cookie 头格式
                        writer.write(cookie.toString())
                        writer.newLine()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadFromFile() {
        if (!cookieFile.exists()) return
//        try {
//            FileInputStream(cookieFile).use { fis ->
//                ObjectInputStream(fis).use { ois ->
//                    val map = ois.readObject() as? HashMap<String, List<Cookie>>
//                    map?.let { cookieStore.putAll(it) }
//                }
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }

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
    fun hasCookies(): Boolean {
        //return cookieStore.values.any { it.isNotEmpty() }

        val now = System.currentTimeMillis()
        // 只要有任何一个未过期的 Cookie，就认为有登录态
        return cookieStore.values.flatten().any { it.expiresAt > now }
    }

    //清除Cookie(退出登录时使用)
    @Synchronized
    fun clear() {
        cookieStore.clear()
        if (cookieFile.exists()) cookieFile.delete()
    }
}