package com.ytclone.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.ytclone.R
import com.ytclone.api.YouTubeApi

class LoginActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var txtStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        txtStatus = findViewById(R.id.txtStatus)

        // زر الإغلاق
        findViewById<ImageButton>(R.id.btnClose)?.setOnClickListener {
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

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                progressBar.visibility = View.GONE
                txtStatus.visibility = View.GONE

                if (url != null) {
                    val isSuccess = !url.contains("ServiceLogin") &&
                            !url.contains("signin/challenge/pwd") &&
                            (url.contains("myaccount.google.com") ||
                             url.contains("youtube.com") ||
                             (url.contains("google.com") && !url.contains("signin")))

                    if (isSuccess) {
                        val cookies = mutableListOf<String>()
                        for (domain in listOf(".google.com", ".youtube.com", "accounts.google.com")) {
                            val c = CookieManager.getInstance().getCookie(domain)
                            if (!c.isNullOrEmpty()) cookies.add(c)
                        }

                        if (cookies.isNotEmpty()) {
                            YouTubeApi.authCookies = cookies.joinToString("; ")
                            val resultIntent = Intent()
                            resultIntent.putExtra("cookies", YouTubeApi.authCookies)
                            setResult(RESULT_OK, resultIntent)
                            finish()
                        }
                    }
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress < 100 && newProgress > 0) {
                    progressBar.progress = newProgress
                    progressBar.visibility = View.VISIBLE
                    txtStatus.text = "جارٍ التحميل... $newProgress%"
                    txtStatus.visibility = View.VISIBLE
                }
            }
        }

        webView.loadUrl("https://accounts.google.com/ServiceLogin?hl=ar&passive=true&continue=https://www.youtube.com&ec=GAZAAQ")
    }
}
