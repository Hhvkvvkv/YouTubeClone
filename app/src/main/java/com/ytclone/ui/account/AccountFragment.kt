package com.ytclone.ui.account

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ytclone.R
import com.ytclone.adapters.MenuAdapter
import com.ytclone.api.YouTubeApi
import com.ytclone.models.MenuItem
import com.ytclone.ui.history.HistoryActivity
import com.ytclone.ui.login.LoginActivity
import com.ytclone.utils.CookieStorage
import android.webkit.CookieManager

class AccountFragment : Fragment() {

    private lateinit var loginSection: LinearLayout
    private lateinit var profileSection: LinearLayout
    private lateinit var btnWebLogin: com.google.android.material.button.MaterialButton
    private lateinit var imgProfile: ImageView
    private lateinit var txtUserName: TextView
    private lateinit var txtUserEmail: TextView
    private lateinit var recyclerMenu: RecyclerView
    private lateinit var prefs: SharedPreferences

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val cookies = result.data?.getStringExtra("cookies") ?: ""
            if (cookies.isNotEmpty()) {
                prefs.edit().putString("youtube_cookies", cookies).apply()
                YouTubeApi.authCookies = cookies
                showProfileSection("مستخدم YouTube", "تم تسجيل الدخول ✓", null)
                Toast.makeText(requireContext(), "✅ تسجيل دخول ناجح - المحتوى المخصص متاح", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "⚠️ لم يتم تسجيل الدخول", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "تم إلغاء تسجيل الدخول", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        prefs = requireContext().getSharedPreferences("videoplus", 0)

        loginSection = view.findViewById(R.id.loginSection)
        profileSection = view.findViewById(R.id.profileSection)
        btnWebLogin = view.findViewById(R.id.btnWebLogin)
        imgProfile = view.findViewById(R.id.imgProfile)
        txtUserName = view.findViewById(R.id.txtUserName)
        txtUserEmail = view.findViewById(R.id.txtUserEmail)
        recyclerMenu = view.findViewById(R.id.recyclerMenu)

        btnWebLogin.setOnClickListener {
            loginLauncher.launch(Intent(requireContext(), LoginActivity::class.java))
        }

        view.findViewById<ImageButton>(R.id.btnSwitchAccount)?.setOnClickListener {
            performLogout()
        }

        setupMenu()
        checkLoginStatus()
    }

    private fun performLogout() {
        // حذف الكوكيز من SharedPreferences
        prefs.edit()
            .remove("youtube_cookies")
            .putBoolean("is_logged_in", false)
            .apply()
        
        // حذف الكوكيز من التخزين الخارجي
        CookieStorage.clearCookies(requireContext())
        
        // مسح الكوكيز من YouTubeApi
        YouTubeApi.authCookies = ""
        
        // مسح كوكيز WebView أيضاً
        try {
            CookieManager.getInstance().removeAllCookies(null)
            CookieManager.getInstance().flush()
        } catch (e: Exception) { }
        
        // عرض شاشة تسجيل الدخول
        showLoginSection()
        
        Toast.makeText(requireContext(), "✅ تم تسجيل الخروج بنجاح", Toast.LENGTH_SHORT).show()
    }

    private fun checkLoginStatus() {
        // أولاً: تحقق من الكوكيز المحفوظة في التخزين الخارجي
        val externalCookies = CookieStorage.loadCookies(requireContext())
        if (!externalCookies.isNullOrEmpty()) {
            YouTubeApi.authCookies = externalCookies
            // حافظ عليها في SharedPreferences أيضاً
            prefs.edit().putString("youtube_cookies", externalCookies).apply()
            showProfileSection("مستخدم YouTube", "تم تسجيل الدخول ✓", null)
            return
        }

        // ثانياً: تحقق من SharedPreferences
        val cookies = prefs.getString("youtube_cookies", "") ?: ""
        val isLoggedIn = prefs.getBoolean("is_logged_in", false)

        if (cookies.isNotEmpty() && isLoggedIn) {
            YouTubeApi.authCookies = cookies
            // حافظ في التخزين الخارجي أيضاً
            CookieStorage.saveCookies(requireContext(), cookies)
            showProfileSection("مستخدم YouTube", "تم تسجيل الدخول ✓", null)
        } else {
            showLoginSection()
        }
    }

    private fun showLoginSection() {
        loginSection.visibility = View.VISIBLE
        profileSection.visibility = View.GONE
    }

    private fun showProfileSection(name: String, email: String, photoUrl: String?) {
        loginSection.visibility = View.GONE
        profileSection.visibility = View.VISIBLE
        txtUserName.text = name
        txtUserEmail.text = email
    }

    private fun setupMenu() {
        val menuItems = listOf(
            MenuItem(R.drawable.ic_history, "السجل", "history"),
            MenuItem(R.drawable.ic_play, "المشاهدة لاحقاً", "watch_later"),
            MenuItem(R.drawable.ic_subscriptions, "قوائم التشغيل", "playlists"),
            MenuItem(R.drawable.ic_play, "الفيديوهات المفضلة", "liked"),
            MenuItem(R.drawable.ic_more, "تسجيل الخروج", "logout"),
        )
        recyclerMenu.layoutManager = LinearLayoutManager(requireContext())
        recyclerMenu.adapter = MenuAdapter(menuItems) { item ->
            navigateToMenu(item.action)
        }
    }

    private fun navigateToMenu(action: String) {
        when (action) {
            "history" -> {
                if (YouTubeApi.authCookies.isEmpty()) {
                    Toast.makeText(requireContext(), "يرجى تسجيل الدخول أولاً", Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(Intent(requireContext(), HistoryActivity::class.java))
                }
            }
            "watch_later" -> Toast.makeText(requireContext(), "المشاهدة لاحقاً - قريباً", Toast.LENGTH_SHORT).show()
            "playlists" -> Toast.makeText(requireContext(), "قوائم التشغيل - قريباً", Toast.LENGTH_SHORT).show()
            "liked" -> Toast.makeText(requireContext(), "المفضلة - قريباً", Toast.LENGTH_SHORT).show()
            "logout" -> performLogout()
            else -> Toast.makeText(requireContext(), action, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance() = AccountFragment()
    }
}
