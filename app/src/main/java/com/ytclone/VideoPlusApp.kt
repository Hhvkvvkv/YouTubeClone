package com.ytclone

import android.app.Application
import android.content.SharedPreferences
import com.ytclone.api.YouTubeApi

class VideoPlusApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // تحميل الكوكيز المحفوظة عند بدء التشغيل
        val prefs: SharedPreferences = getSharedPreferences("videoplus", 0)
        val savedCookies = prefs.getString("youtube_cookies", "") ?: ""
        if (savedCookies.isNotEmpty()) {
            YouTubeApi.authCookies = savedCookies
        }
    }
}
