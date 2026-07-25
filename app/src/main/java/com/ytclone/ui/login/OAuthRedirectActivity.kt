package com.ytclone.ui.login

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ytclone.api.YouTubeApi
import com.ytclone.utils.CookieStorage

class OAuthRedirectActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uri = intent?.data
        if (uri != null) {
            val code = uri.getQueryParameter("code")
            if (code != null) {
                // تم الحصول على كود التفويض
                handleAuthCode(code)
            } else {
                Toast.makeText(this, "❌ لم يتم الحصول على كود التفويض", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "❌ لا يوجد بيانات إعادة توجيه", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun handleAuthCode(code: String) {
        // في هذا المثال، سنحفظ الكود كبديل
        // في التطبيق الحقيقي، يجب تبادل الكود مع خادم Google للحصول على Access Token
        YouTubeApi.authCookies = "oauth_code=$code"
        CookieStorage.saveCookies(this, YouTubeApi.authCookies)

        val prefs = getSharedPreferences("videoplus", 0)
        prefs.edit()
            .putString("youtube_cookies", YouTubeApi.authCookies)
            .putBoolean("is_logged_in", true)
            .apply()

        Toast.makeText(this, "✅ تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()

        // العودة للتطبيق الرئيسي
        val intent = Intent(this, com.ytclone.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }
}
