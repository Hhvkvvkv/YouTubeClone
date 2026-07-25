package com.ytclone.utils

import android.content.Context
import android.os.Environment
import com.ytclone.api.YouTubeApi
import java.io.File

object CookieStorage {

    private const val COOKIE_DIR = "VideoPlus"
    private const val COOKIE_FILE = "cookies.txt"

    // الحصول على مسار مجلد التخزين الخارجي
    private fun getExternalDir(context: Context): File {
        val dir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            COOKIE_DIR
        )
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // حفظ الكوكيز في ملف خارجي
    fun saveCookies(context: Context, cookies: String) {
        try {
            val dir = getExternalDir(context)
            val file = File(dir, COOKIE_FILE)
            file.writeText(cookies)
            android.util.Log.d("CookieStorage", "✅ تم حفظ الكوكيز في: ${file.absolutePath}")
        } catch (e: Exception) {
            android.util.Log.e("CookieStorage", "❌ خطأ في حفظ الكوكيز: ${e.message}")
        }
    }

    // تحميل الكوكيز من الملف الخارجي
    fun loadCookies(context: Context): String? {
        try {
            val dir = getExternalDir(context)
            val file = File(dir, COOKIE_FILE)
            if (file.exists()) {
                val cookies = file.readText()
                if (cookies.isNotEmpty()) {
                    android.util.Log.d("CookieStorage", "✅ تم تحميل الكوكيز من: ${file.absolutePath}")
                    return cookies
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("CookieStorage", "❌ خطأ في تحميل الكوكيز: ${e.message}")
        }
        return null
    }

    // التحقق من وجود كوكيز محفوظة
    fun hasCookies(context: Context): Boolean {
        val dir = getExternalDir(context)
        val file = File(dir, COOKIE_FILE)
        return file.exists() && file.readText().isNotEmpty()
    }

    // حذف الكوكيز المحفوظة
    fun clearCookies(context: Context) {
        try {
            val dir = getExternalDir(context)
            val file = File(dir, COOKIE_FILE)
            if (file.exists()) {
                file.delete()
                android.util.Log.d("CookieStorage", "✅ تم حذف الكوكيز من التخزين الخارجي")
            }
        } catch (e: Exception) {
            android.util.Log.e("CookieStorage", "❌ خطأ في حذف الكوكيز: ${e.message}")
        }
    }
}
