package com.example.wisatajogja.data.session

import android.content.Context
import android.content.SharedPreferences
import com.example.wisatajogja.data.model.User
import org.json.JSONObject

object SessionManager {
    private const val PREFS_NAME = "wisata_jogja_session"
    private const val KEY_TOKEN = "api_token"
    private const val KEY_USER = "user_json"
    private const val KEY_GUEST = "guest_mode"
    private const val KEY_NOTIFICATIONS = "notifications_enabled"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    val token: String?
        get() = prefs.getString(KEY_TOKEN, null)

    val isLoggedIn: Boolean
        get() = !token.isNullOrBlank() && currentUser != null

    val isGuest: Boolean
        get() = prefs.getBoolean(KEY_GUEST, false) && !isLoggedIn

    val isAdmin: Boolean
        get() = currentUser?.isAdmin == true

    var currentUser: User?
        get() = prefs.getString(KEY_USER, null)?.let { raw ->
            runCatching { User.fromJson(JSONObject(raw)) }.getOrNull()
        }
        private set(value) {
            prefs.edit().apply {
                if (value == null) remove(KEY_USER) else putString(KEY_USER, value.toJson().toString())
            }.apply()
        }

    var notificationsEnabled: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATIONS, true)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATIONS, value).apply()

    fun saveSession(token: String, user: User) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_USER, user.toJson().toString())
            .putBoolean(KEY_GUEST, false)
            .apply()
    }

    fun updateUser(user: User) {
        currentUser = user
    }

    fun enterGuestMode() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).putBoolean(KEY_GUEST, true).apply()
    }

    fun clear() {
        prefs.edit().remove(KEY_TOKEN).remove(KEY_USER).putBoolean(KEY_GUEST, false).apply()
    }
}
