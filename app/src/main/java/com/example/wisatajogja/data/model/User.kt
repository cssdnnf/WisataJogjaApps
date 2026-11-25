package com.example.wisatajogja.data.model

import org.json.JSONObject

data class UserStats(
    val visited: Int = 0,
    val saved: Int = 0,
    val reviews: Int = 0,
)

data class User(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val phone: String,
    val city: String,
    val bio: String,
    val role: String,
    val status: String,
    val photoUrl: String?,
    val stats: UserStats,
) {
    val isAdmin: Boolean get() = role == "admin"

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("nama", name)
        put("username", username)
        put("email", email)
        put("telepon", phone)
        put("domisili", city)
        put("bio", bio)
        put("role", role)
        put("status", status)
        put("foto_profil", photoUrl ?: JSONObject.NULL)
        put("statistik", JSONObject().apply {
            put("dikunjungi", stats.visited)
            put("tersimpan", stats.saved)
            put("ulasan", stats.reviews)
        })
    }

    companion object {
        fun fromJson(json: JSONObject): User {
            val statsJson = json.optJSONObject("statistik")
            return User(
                id = json.optInt("id"),
                name = json.optString("nama"),
                username = json.optString("username"),
                email = json.optString("email"),
                phone = json.optString("telepon"),
                city = json.optString("domisili"),
                bio = json.optString("bio"),
                role = json.optString("role", "user"),
                status = json.optString("status", "aktif"),
                photoUrl = json.stringOrNull("foto_profil"),
                stats = UserStats(
                    visited = statsJson?.optInt("dikunjungi") ?: 0,
                    saved = statsJson?.optInt("tersimpan") ?: 0,
                    reviews = statsJson?.optInt("ulasan") ?: 0,
                ),
            )
        }
    }
}

internal fun JSONObject.stringOrNull(key: String): String? {
    if (isNull(key)) return null
    val value = optString(key)
    return value.takeIf { it.isNotBlank() && it != "null" }
}
