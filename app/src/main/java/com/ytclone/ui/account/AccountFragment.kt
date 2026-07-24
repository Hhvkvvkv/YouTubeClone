package com.ytclone.ui.account

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.ytclone.R
import com.ytclone.adapters.MenuAdapter
import com.ytclone.models.MenuItem

class AccountFragment : Fragment() {

    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loginSection: LinearLayout
    private lateinit var profileSection: LinearLayout
    private lateinit var btnGoogleSignIn: MaterialButton
    private lateinit var imgProfile: ImageView
    private lateinit var txtUserName: TextView
    private lateinit var txtUserEmail: TextView
    private lateinit var recyclerMenu: RecyclerView

    companion object {
        private const val RC_SIGN_IN = 9001
        fun newInstance() = AccountFragment()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_account, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loginSection = view.findViewById(R.id.loginSection)
        profileSection = view.findViewById(R.id.profileSection)
        btnGoogleSignIn = view.findViewById(R.id.btnGoogleSignIn)
        imgProfile = view.findViewById(R.id.imgProfile)
        txtUserName = view.findViewById(R.id.txtUserName)
        txtUserEmail = view.findViewById(R.id.txtUserEmail)
        recyclerMenu = view.findViewById(R.id.recyclerMenu)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestIdToken(getString(R.string.google_api_key))
            .requestServerAuthCode(getString(R.string.google_api_key))
            .build()

        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        btnGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        view.findViewById<ImageButton>(R.id.btnSwitchAccount)?.setOnClickListener {
            googleSignInClient.signOut().addOnCompleteListener {
                showLoginSection()
            }
        }

        setupMenu()
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        val account = GoogleSignIn.getLastSignedInAccount(requireContext())
        if (account != null) {
            showProfileSection(account.displayName ?: "مستخدم", account.email ?: "", account.photoUrl?.toString())
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

        if (!photoUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .into(imgProfile)
        }
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
            Toast.makeText(requireContext(), item.title, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                showProfileSection(account.displayName ?: "مستخدم", account.email ?: "", account.photoUrl?.toString())
                Toast.makeText(requireContext(), "تم تسجيل الدخول بنجاح: ${account.email}", Toast.LENGTH_LONG).show()
            } catch (e: ApiException) {
                Toast.makeText(requireContext(), "فشل تسجيل الدخول: ${e.statusCode}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
