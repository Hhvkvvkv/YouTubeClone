package com.ytclone.ui.login

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.*
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ytclone.R
import com.ytclone.api.YouTubeApi

class LoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtStatus: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var isLoginComplete = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        txtStatus = findViewById(R.id.txtStatus)

        findViewById<ImageButton>(R.id.btnClose)?.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        setupWebView()
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.databaseEnabled = true
        webView.settings.useWideViewPort = true
        webView.settings.setSupportMultipleWindows(true)
        webView.settings.javaScriptCanOpenWindowsAutomatically = true
        webView.settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        webView.settings.setSupportZoom(false)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null) {
                    // تحقق إذا وصلنا لـ YouTube بعد تسجيل الدخول
                    val isYouTube = url.contains("youtube.com") && !url.contains("accounts.google.com")
                    val isLoginPage = url.contains("accounts.google.com/ServiceLogin") ||
                            url.contains("accounts.google.com/signin")

                    if (isYouTube && !isLoginPage && !isLoginComplete) {
                        // نجاح تسجيل الدخول!
                        isLoginComplete = true
                        handler.postDelayed({
                            saveCookiesAndFinish()
                        }, 3000) // انتظر 3 ثواني لضمان تعيين الكوكيز
                    } else if (isLoginPage) {
                        updateStatus("أدخل البريد وكلمة المرور")
                    }
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
                if (newProgress == 100) {
                    progressBar.visibility = View.GONE
                    txtStatus.visibility = View.GONE
                } else {
                    progressBar.visibility = View.VISIBLE
                    txtStatus.visibility = View.VISIBLE
                }
            }
        }

        progressBar.visibility = View.GONE
        txtStatus.visibility = View.GONE

        // تحميل صفحة تسجيل الدخول
        webView.loadUrl("https://accounts.google.com/ServiceLogin?hl=ar&passive=true&continue=https://www.youtube.com/&ec=GAZAAQ")
    }

    private fun saveCookiesAndFinish() {
        val cookieManager = CookieManager.getInstance()
        val allCookies = mutableListOf<String>()

        // جمع الكوكيز من جميع النطاقات
        for (domain in listOf(".google.com", ".youtube.com", "accounts.google.com", "google.com", "youtube.com")) {
            try {
                val c = cookieManager.getCookie(domain)
                if (!c.isNullOrEmpty()) {
                    allCookies.add(c)
                }
            } catch (e: Exception) { }
        }

        if (allCookies.isNotEmpty()) {
            YouTubeApi.authCookies = allCookies.joinToString("; ")

            // حفظ في SharedPreferences
            val prefs = getSharedPreferences("videoplus", 0)
            prefs.edit()
                .putString("youtube_cookies", YouTubeApi.authCookies)
                .putBoolean("is_logged_in", true)
                .apply()

            runOnUiThread {
                Toast.makeText(this, "✅ تم تسجيل الدخول", Toast.LENGTH_SHORT).show()
            }

            val resultIntent = Intent()
            resultIntent.putExtra("cookies", YouTubeApi.authCookies)
            resultIntent.putExtra("logged_in", true)
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            runOnUiThread {
                Toast.makeText(this, "⚠️ حاول مرة أخرى", Toast.LENGTH_LONG).show()
            }
            // إعادة تحميل الصفحة
            handler.postDelayed({
                isLoginComplete = false
                webView.loadUrl("https://accounts.google.com/ServiceLogin?hl=ar&passive=true&continue=https://www.youtube.com/&ec=GAZAAQ")
            }, 2000)
        }
    }

    private fun updateStatus(msg: String) {
        runOnUiThread { txtStatus.text = msg }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack()
        else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }
}
