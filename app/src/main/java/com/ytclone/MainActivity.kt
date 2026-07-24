package com.ytclone

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.ytclone.ui.account.AccountFragment
import com.ytclone.ui.home.HomeFragment
import com.ytclone.ui.shorts.ShortsFragment
import com.ytclone.ui.subscriptions.SubscriptionsFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (savedInstanceState == null) {
            loadFragment(HomeFragment.newInstance())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment.newInstance())
                    true
                }
                R.id.nav_shorts -> {
                    loadFragment(ShortsFragment.newInstance())
                    true
                }
                R.id.nav_subscriptions -> {
                    loadFragment(SubscriptionsFragment.newInstance())
                    true
                }
                R.id.nav_account -> {
                    loadFragment(AccountFragment.newInstance())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
