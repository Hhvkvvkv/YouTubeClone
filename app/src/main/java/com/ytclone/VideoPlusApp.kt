package com.ytclone

import android.app.Application
import com.ytclone.api.YouTubeApi
import com.ytclone.utils.CookieStorage

class VideoPlusApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // محاولة تحميل الكوكيز من التخزين الخارجي أولاً
        val externalCookies = CookieStorage.loadCookies(this)
        if (!externalCookies.isNullOrEmpty()) {
            YouTubeApi.authCookies = externalCookies
            android.util.Log.d("VideoPlusApp", "✅ تم تحميل الكوكيز من التخزين الخارجي")
            return
        }

        // إذا لم توجد في التخزين الخارجي، حاول SharedPreferences
        val prefs = getSharedPreferences("videoplus", 0)
        val savedCookies = prefs.getString("youtube_cookies", "") ?: ""
        if (savedCookies.isNotEmpty()) {
            YouTubeApi.authCookies = savedCookies
            // حفظها في التخزين الخارجي للمرة القادمة
            CookieStorage.saveCookies(this, savedCookies)
            android.util.Log.d("VideoPlusApp", "✅ تم تحميل الكوكيز من SharedPreferences")
        }
    }
}
