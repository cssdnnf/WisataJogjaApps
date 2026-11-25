package com.example.wisatajogja.ui.admin

import android.content.Intent
import android.os.Bundle
import com.example.wisatajogja.R
import com.example.wisatajogja.data.session.SessionManager
import com.example.wisatajogja.databinding.ActivityAdminMainBinding
import com.example.wisatajogja.ui.auth.LoginActivity
import com.example.wisatajogja.ui.base.BaseActivity
import com.example.wisatajogja.ui.navigation.AdminNavigator

class AdminMainActivity : BaseActivity() {

    private lateinit var binding: ActivityAdminMainBinding
    private lateinit var navigator: AdminNavigator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!SessionManager.isAdmin) {
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
            return
        }
        binding = ActivityAdminMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyInsets(binding.root)

        navigator = AdminNavigator(
            activity = this,
            containerId = R.id.fragmentContainer,
            current = AdminNavigator.restoreTab(savedInstanceState?.getString(STATE_TAB)),
        )
        navigator.configureBottomNavigation(findViewById(R.id.bottomNavCard))
        navigator.start()
    }

    fun openTab(tab: com.example.wisatajogja.ui.navigation.AdminTab) {
        navigator.navigate(tab)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(STATE_TAB, navigator.currentTab().name)
        super.onSaveInstanceState(outState)
    }

    companion object {
        private const val STATE_TAB = "admin_selected_tab"
    }
}
