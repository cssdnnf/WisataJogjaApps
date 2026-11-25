package com.example.wisatajogja

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.wisatajogja.data.session.SessionManager

class WisataJogjaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        SessionManager.init(this)
    }
}
