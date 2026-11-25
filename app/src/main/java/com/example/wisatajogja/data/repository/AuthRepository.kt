package com.example.wisatajogja.data.repository

import com.example.wisatajogja.data.model.User
import com.example.wisatajogja.data.remote.ApiClient
import com.example.wisatajogja.data.session.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AuthResult(val token: String, val user: User)
data class ForgotPasswordResult(val resetCode: String?)

class AuthRepository {
    suspend fun login(email: String, password: String): AuthResult {
        val response = ApiClient.post(
            action = "login",
            body = JSONObject().apply {
                put("email", email.trim())
                put("password", password)
            },
        )
        val payload = response.data as? JSONObject ?: error("Respons login tidak valid.")
        return AuthResult(
            token = payload.optString("token"),
            user = User.fromJson(payload.optJSONObject("user") ?: JSONObject()),
        )
    }

    suspend fun register(name: String, email: String, password: String): AuthResult {
        val response = ApiClient.post(
            action = "register",
            body = JSONObject().apply {
                put("nama", name.trim())
                put("email", email.trim())
                put("password", password)
            },
        )
        val payload = response.data as? JSONObject ?: error("Respons pendaftaran tidak valid.")
        return AuthResult(
            token = payload.optString("token"),
            user = User.fromJson(payload.optJSONObject("user") ?: JSONObject()),
        )
    }

    suspend fun forgotPassword(email: String): ForgotPasswordResult {
        val response = ApiClient.post(
            action = "forgot-password",
            body = JSONObject().put("email", email.trim()),
        )
        val payload = response.data as? JSONObject
        return ForgotPasswordResult(payload?.optString("reset_code")?.takeIf { it.isNotBlank() })
    }

    suspend fun resetPassword(email: String, code: String, password: String): String {
        val response = ApiClient.post(
            action = "reset-password",
            body = JSONObject().apply {
                put("email", email.trim())
                put("code", code.trim())
                put("password", password)
            },
        )
        return response.message
    }

    suspend fun refreshCurrentUser(): User {
        val response = ApiClient.get("me", token = SessionManager.token)
        return User.fromJson(response.data as? JSONObject ?: JSONObject())
    }

    suspend fun logout() {
        runCatching { ApiClient.post("logout", token = SessionManager.token) }
        SessionManager.clear()
    }

    companion object {
        suspend fun initializeSession(result: AuthResult) = withContext(Dispatchers.Main) {
            SessionManager.saveSession(result.token, result.user)
        }
    }
}
