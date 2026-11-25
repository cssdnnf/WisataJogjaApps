package com.example.wisatajogja.ui.navigation

import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.wisatajogja.R
import com.example.wisatajogja.ui.main.beranda.BerandaFragment
import com.example.wisatajogja.ui.main.profil.ProfilFragment
import com.example.wisatajogja.ui.main.rekomendasi.RekomendasiFragment
import com.example.wisatajogja.ui.main.tersimpan.TersimpanFragment

enum class MainTab(val index: Int, val tag: String) {
    HOME(0, "tab_beranda"),
    EXPLORE(1, "tab_rekomendasi"),
    SAVED(2, "tab_tersimpan"),
    PROFILE(3, "tab_profil"),
}

class MainNavigator(
    private val activity: FragmentActivity,
    @IdRes private val containerId: Int,
    private var current: MainTab = MainTab.HOME,
) {
    var onTabChanged: ((MainTab) -> Unit)? = null

    fun start() {
        show(current, false)
    }

    fun navigate(tab: MainTab) {
        if (tab == current && activity.supportFragmentManager.findFragmentById(containerId) != null) {
            onTabChanged?.invoke(tab)
            return
        }
        show(tab, true)
    }

    fun navigateToExplore(category: String? = null, area: String? = null) {
        current = MainTab.EXPLORE
        activity.supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(containerId, RekomendasiFragment.newInstance(category, area), MainTab.EXPLORE.tag)
            .commit()
        onTabChanged?.invoke(MainTab.EXPLORE)
    }

    private fun show(tab: MainTab, animate: Boolean) {
        current = tab
        val transaction = activity.supportFragmentManager.beginTransaction()
        if (animate) {
            transaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        transaction.replace(containerId, fragmentFor(tab), tab.tag)
        transaction.commit()
        onTabChanged?.invoke(tab)
    }

    private fun fragmentFor(tab: MainTab): Fragment = when (tab) {
        MainTab.HOME -> BerandaFragment()
        MainTab.EXPLORE -> RekomendasiFragment()
        MainTab.SAVED -> TersimpanFragment()
        MainTab.PROFILE -> ProfilFragment()
    }

    companion object {
        fun restoreTab(name: String?): MainTab =
            MainTab.entries.firstOrNull { it.name == name } ?: MainTab.HOME
    }

    fun configureBottomNavigation(root: android.view.View) {
        val controller = BottomNavController(
            root = root.findViewById(R.id.bottomNavCard),
            items = listOf(
                BottomNavItem(R.id.navItemHome, R.id.navIconHome, R.id.navLabelHome),
                BottomNavItem(R.id.navItemExplore, R.id.navIconExplore, R.id.navLabelExplore),
                BottomNavItem(R.id.navItemSaved, R.id.navIconSaved, R.id.navLabelSaved),
                BottomNavItem(R.id.navItemProfile, R.id.navIconProfile, R.id.navLabelProfile),
            ),
            activeBackground = R.drawable.bg_nav_active,
            activeContentColor = R.color.cream_light,
            inactiveContentColor = R.color.muted,
        ) { index -> navigate(MainTab.entries[index]) }
        controller.select(current.index)
        onTabChanged = { tab -> controller.select(tab.index) }
    }

    fun currentTab(): MainTab = current
}
