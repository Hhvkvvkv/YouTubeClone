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
import com.ytclone.ui.login.LoginActivity
import com.ytclone.ui.history.HistoryActivity

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
                showProfileSection("مستخدم YouTube", "تم تسجيل الدخول", null)
                Toast.makeText(requireContext(), "✅ تم تسجيل الدخول بنجاح", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "⚠️ لم يتم الحصول على الكوكيز", Toast.LENGTH_LONG).show()
            }
        } else {
            Toast.makeText(requireContext(), "❌ تم إلغاء تسجيل الدخول", Toast.LENGTH_SHORT).show()
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
            prefs.edit().remove("youtube_cookies").apply()
            YouTubeApi.authCookies = ""
            showLoginSection()
            Toast.makeText(requireContext(), "تم تسجيل الخروج", Toast.LENGTH_SHORT).show()
        }

        setupMenu()
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        val cookies = prefs.getString("youtube_cookies", "") ?: ""
        if (cookies.isNotEmpty()) {
            YouTubeApi.authCookies = cookies
            showProfileSection("مستخدم YouTube", "تم تسجيل الدخول", null)
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
        )
        recyclerMenu.layoutManager = LinearLayoutManager(requireContext())
        recyclerMenu.adapter = MenuAdapter(menuItems) { item ->
            navigateToMenu(item.action)
        }
    }

    private fun navigateToMenu(action: String) {
        when (action) {
            "history" -> startActivity(Intent(requireContext(), HistoryActivity::class.java))
            else -> Toast.makeText(requireContext(), when(action) {
                "watch_later" -> "المشاهدة لاحقاً"
                "playlists" -> "قوائم التشغيل"
                "liked" -> "الفيديوهات المفضلة"
                else -> action
            }, Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun newInstance() = AccountFragment()
    }
}
