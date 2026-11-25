package com.example.wisatajogja.data.repository

import com.example.wisatajogja.data.model.Reservation
import com.example.wisatajogja.data.model.User
import com.example.wisatajogja.data.remote.ApiClient
import com.example.wisatajogja.data.session.SessionManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class ProfileRepository {
    suspend fun updateProfile(
        name: String,
        username: String,
        phone: String,
        city: String,
        bio: String,
    ): User {
        val response = ApiClient.post(
            "profil-update",
            JSONObject().apply {
                put("nama", name.trim())
                put("username", username.trim().lowercase())
                put("telepon", phone.trim())
                put("domisili", city.trim())
                put("bio", bio.trim())
            },
            SessionManager.token,
        )
        val user = User.fromJson(response.data as? JSONObject ?: JSONObject())
        SessionManager.updateUser(user)
        return user
    }

    suspend fun uploadPhoto(file: File): String {
        val response = ApiClient.upload(
            action = "profil-foto",
            file = file,
            fieldName = "foto",
            token = SessionManager.token,
        )
        val url = (response.data as? JSONObject)?.optString("foto_profil").orEmpty()
        SessionManager.currentUser?.let { SessionManager.updateUser(it.copy(photoUrl = url)) }
        return url
    }

    suspend fun changePassword(current: String, newPassword: String): String {
        val response = ApiClient.post(
            "profil-password",
            JSONObject().apply {
                put("password_lama", current)
                put("password_baru", newPassword)
            },
            SessionManager.token,
        )
        return response.message
    }

    suspend fun reservations(): List<Reservation> {
        val response = ApiClient.get("reservasi", token = SessionManager.token)
        val array = response.data as? JSONArray ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                array.optJSONObject(i)?.let { add(Reservation.fromJson(it)) }
            }
        }
    }
}

class ReservationRepository {
    suspend fun create(
        destinationId: Int,
        visitDate: String,
        people: Int,
        notes: String,
    ): JSONObject {
        val response = ApiClient.post(
            "reservasi-buat",
            JSONObject().apply {
                put("destinasi_id", destinationId)
                put("tanggal_kunjungan", visitDate)
                put("jumlah_orang", people)
                put("catatan", notes.trim())
            },
            SessionManager.token,
        )
        return response.data as? JSONObject ?: JSONObject()
    }
}
