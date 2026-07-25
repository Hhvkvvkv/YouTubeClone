package com.ytclone.ui.login

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.ytclone.R
import com.ytclone.api.YouTubeApi
import com.ytclone.utils.CookieStorage

class LoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtStatus: TextView
    private val handler = Handler(Looper.getMainLooper())
    private var isLoginComplete = false
    private var loginAttempts = 0

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

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.visibility = View.VISIBLE

        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            allowFileAccess = false
            allowContentAccess = false
            
            // استخدام User-Agent يحاكي متصفح Chrome الحقيقي
            userAgentString = "Mozilla/5.0 (Linux; Android 13; SM-S908B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        
        // مسح الكوكيز القديمة أولاً
        cookieManager.removeAllCookies(null)
        cookieManager.flush()

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                progressBar.visibility = View.VISIBLE
                txtStatus.visibility = View.VISIBLE
                txtStatus.text = "جارٍ التحميل..."
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                
                if (url != null) {
                    Log.d("LoginActivity", "URL: $url")
                    
                    // التحقق من أننا على صفحة يوتيوب (بعد تسجيل الدخول)
                    val isYouTube = url.contains("youtube.com") && 
                        !url.contains("accounts.google.com") &&
                        !url.contains(" consent")
                    val isLoginPage = url.contains("accounts.google.com") ||
                            url.contains("accounts.google.com/signin")

                    if (isYouTube && !isLoginComplete) {
                        isLoginComplete = true
                        txtStatus.text = "تم تسجيل الدخول بنجاح!"
                        handler.postDelayed({
                            saveCookiesAndFinish()
                        }, 2000)
                    } else if (isLoginPage) {
                        // إخفاء تحذير "متصفح غير آمن" باستخدام JavaScript
                        hideUnsafeBrowserWarning(view)
                        txtStatus.text = "أدخل البريد الإلكتروني وكلمة المرور"
                    }
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url?.toString() ?: return false
                // السماح فقط بروابط Google و YouTube
                if (url.contains("google.com") || url.contains("youtube.com")) {
                    return false
                }
                return true
            }

            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                txtStatus.text = "خطأ في الاتصال - جارٍ إعادة المحاولة..."
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                progressBar.progress = newProgress
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                return true
            }
        }

        progressBar.visibility = View.GONE
        txtStatus.visibility = View.GONE

        // فتح صفحة تسجيل الدخول
        webView.loadUrl("https://accounts.google.com/ServiceLogin?hl=ar&passive=true&continue=https://www.youtube.com/&ec=GAZAAQ")
    }

    // إخفاء تحذير "متصفح غير آمن"
    private fun hideUnsafeBrowserWarning(view: WebView?) {
        val js = """
            (function() {
                // إخفاء رسالة "قد يكون هذا المتصفح أو التطبيق غير آمن"
                var elements = document.querySelectorAll('*');
                for (var i = 0; i < elements.length; i++) {
                    var text = elements[i].textContent || '';
                    if (text.includes('غير آمن') || text.includes('unsafe') || text.includes('آمن') || text.includes('secure')) {
                        elements[i].style.display = 'none';
                        elements[i].style.visibility = 'hidden';
                        elements[i].style.height = '0';
                        elements[i].style.overflow = 'hidden';
                    }
                }
                // إخفاء العناصر التي تحتوي على تحذيرات
                var warningDivs = document.querySelectorAll('[data-is-caution], [role="alert"], .oYZgdc, .d60ED');
                for (var j = 0; j < warningDivs.length; j++) {
                    warningDivs[j].style.display = 'none';
                }
            })();
        """.trimIndent()
        view?.evaluateJavascript(js, null)
    }

    private fun saveCookiesAndFinish() {
        val cookieManager = CookieManager.getInstance()
        val allCookies = mutableListOf<String>()

        for (domain in listOf(".google.com", ".youtube.com", "accounts.google.com", "google.com", "youtube.com")) {
            try {
                val c = cookieManager.getCookie(domain)
                if (!c.isNullOrEmpty()) {
                    allCookies.add(c)
                }
            } catch (e: Exception) { }
        }

        if (allCookies.isNotEmpty()) {
            val cookieString = allCookies.joinToString("; ")
            YouTubeApi.authCookies = cookieString

            // حفظ في التخزين الخارجي
            CookieStorage.saveCookies(this, cookieString)

            // حفظ في SharedPreferences
            val prefs = getSharedPreferences("videoplus", 0)
            prefs.edit()
                .putString("youtube_cookies", cookieString)
                .putBoolean("is_logged_in", true)
                .apply()

            runOnUiThread {
                Toast.makeText(this, "✅ تم تسجيل الدخول بنجاح", Toast.LENGTH_SHORT).show()
            }

            val resultIntent = Intent()
            resultIntent.putExtra("cookies", cookieString)
            resultIntent.putExtra("logged_in", true)
            setResult(RESULT_OK, resultIntent)
            finish()
        } else {
            runOnUiThread {
                Toast.makeText(this, "⚠️ حاول مرة أخرى - لم يتم حفظ الكوكيز", Toast.LENGTH_LONG).show()
            }
            handler.postDelayed({
                isLoginComplete = false
                webView.loadUrl("https://accounts.google.com/ServiceLogin?hl=ar&passive=true&continue=https://www.youtube.com/&ec=GAZAAQ")
            }, 2000)
        }
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.destroy()
    }
}
