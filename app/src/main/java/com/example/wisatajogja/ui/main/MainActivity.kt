package com.example.wisatajogja.ui.main

import android.content.Intent
import android.os.Bundle
import com.example.wisatajogja.R
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.databinding.ActivityMainBinding
import com.example.wisatajogja.ui.auth.LoginActivity
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.ui.navigation.MainNavigator
import com.example.wisatajogja.ui.navigation.MainTab

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navigator: MainNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isLoggedIn && !SessionManager.isGuest) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        val restored = savedInstanceState?.getString(STATE_TAB)
        navigator = MainNavigator(
            activity = this,
            containerId = R.id.fragmentContainer,
            current = MainNavigator.restoreTab(restored),
        )
        navigator.configureBottomNavigation(findViewById(R.id.bottomNavCard))
        navigator.start()
    }

    fun openExplore(category: String? = null, area: String? = null) {
        navigator.navigateToExplore(category, area)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_TAB, navigator.currentTab().name)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val STATE_TAB = "main_selected_tab"
    }
}
