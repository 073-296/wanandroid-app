package com.study.wanandroid.network

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class PersistentCookieJar(context: Context) : CookieJar {
    private val cookieFile = File(context.filesDir, "cookies.json")

    //内存缓存+文件持久化
    private val cookieStore = mutableMapOf<String, List<Cookie>>()

    init {
        loadFromFile()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        return cookieStore[url.host] ?: emptyList()
    }

    @Synchronized
    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>
    ) {
        cookieStore[url.host] = cookies
        saveToFile()
    }

    //文件读写
    private fun saveToFile() {
        try {
            ObjectOutputStream(FileOutputStream(cookieFile)).use { oos ->
                oos.writeObject(HashMap(cookieStore))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadFromFile() {
        if (!cookieFile.exists()) return
        try {
            FileInputStream(cookieFile).use { fis ->
                ObjectInputStream(fis).use { ois ->
                    val map = ois.readObject() as? HashMap<String, List<Cookie>>
                    map?.let { cookieStore.putAll(it) }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //清除Cookie(退出登录时使用)
    @Synchronized
    fun clear(){
        cookieStore.clear()
        if (cookieFile.exists()) cookieFile.delete()
    }
}