package com.example.wisatajogja.ui.navigation

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.wisatajogja.R
import com.example.wisatajogja.ui.admin.akun.AkunAdminFragment
import com.example.wisatajogja.ui.admin.pengguna.PenggunaFragment
import com.example.wisatajogja.ui.admin.ringkasan.RingkasanFragment
import com.example.wisatajogja.ui.admin.wisata.KelolaWisataFragment

enum class AdminTab(val index: Int, val tag: String) {
    SUMMARY(0, "tab_ringkasan"),
    DESTINATIONS(1, "tab_kelola_wisata"),
    USERS(2, "tab_pengguna"),
    ACCOUNT(3, "tab_akun_admin"),
}

class AdminNavigator(
    private val activity: FragmentActivity,
    @IdRes private val containerId: Int,
    private var current: AdminTab = AdminTab.SUMMARY,
) {
    var onTabChanged: ((AdminTab) -> Unit)? = null

    fun start() {
        show(current, false)
    }

    fun navigate(tab: AdminTab) {
        if (tab == current && activity.supportFragmentManager.findFragmentById(containerId) != null) {
            onTabChanged?.invoke(tab)
            return
        }
        show(tab, true)
    }

    private fun show(tab: AdminTab, animate: Boolean) {
        current = tab
        val transaction = activity.supportFragmentManager.beginTransaction()
        if (animate) {
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        transaction.replace(containerId, fragmentFor(tab), tab.tag)
        transaction.commit()
        onTabChanged?.invoke(tab)
    }

    private fun fragmentFor(tab: AdminTab): Fragment = when (tab) {
        AdminTab.SUMMARY -> RingkasanFragment()
        AdminTab.DESTINATIONS -> KelolaWisataFragment()
        AdminTab.USERS -> PenggunaFragment()
        AdminTab.ACCOUNT -> AkunAdminFragment()
    }

    fun configureBottomNavigation(root: android.view.View) {
        val controller = BottomNavController(
            root = root.findViewById(R.id.bottomNavCard),
            items = listOf(
                BottomNavItem(R.id.navItemSummary, R.id.navIconSummary, R.id.navLabelSummary),
                BottomNavItem(R.id.navItemDestinations, R.id.navIconDestinations, R.id.navLabelDestinations),
                BottomNavItem(R.id.navItemUsers, R.id.navIconUsers, R.id.navLabelUsers),
                BottomNavItem(R.id.navItemAccount, R.id.navIconAccount, R.id.navLabelAccount),
            ),
            activeBackground = R.drawable.bg_nav_active_gold,
            activeContentColor = R.color.green_dark,
            inactiveContentColor = R.color.muted,
        ) { index -> navigate(AdminTab.entries[index]) }
        controller.select(current.index)
        onTabChanged = { tab -> controller.select(tab.index) }
    }

    companion object {
        fun restoreTab(name: String?): AdminTab =
            AdminTab.entries.firstOrNull { it.name == name } ?: AdminTab.SUMMARY
    }

    fun currentTab(): AdminTab = current
}
