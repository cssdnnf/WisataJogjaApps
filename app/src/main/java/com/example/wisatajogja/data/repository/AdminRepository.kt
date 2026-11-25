package com.example.wisatajogja.data.repository

import com.example.wisatajogja.data.model.AdminSummary
import com.example.wisatajogja.data.model.AdminUser
import com.example.wisatajogja.data.model.AdminUserPage
import com.example.wisatajogja.data.model.Destination
import com.example.wisatajogja.data.remote.ApiClient
import com.example.wisatajogja.data.session.SessionManager
import org.json.JSONArray
import org.json.JSONObject

data class DestinationForm(
    val id: Int = 0,
    val name: String,
    val category: String,
    val area: String,
    val shortDescription: String,
    val description: String,
    val imageUrl: String,
    val ticketPrice: Int,
    val openHours: String,
    val latitude: Double,
    val longitude: Double,
    val published: Boolean,
    val featured: Boolean,
)

class AdminRepository {
    private val token: String? get() = SessionManager.token

    suspend fun summary(): AdminSummary {
        val response = ApiClient.get("admin-ringkasan", token = token)
        return AdminSummary.fromJson(response.data as? JSONObject ?: JSONObject())
    }

    suspend fun users(query: String = "", status: String = "", page: Int = 1): AdminUserPage {
        val params = buildMap {
            put("page", page.toString())
            if (query.isNotBlank()) put("q", query)
            if (status.isNotBlank()) put("status", status)
        }
        val response = ApiClient.get("admin-pengguna", params, token)
        val data = response.data as? JSONObject ?: return AdminUserPage(emptyList(), 0, 1)
        val items = buildList {
            data.optJSONArray("items")?.let { array ->
                for (i in 0 until array.length()) {
                    array.optJSONObject(i)?.let { add(AdminUser.fromJson(it)) }
                }
            }
        }
        return AdminUserPage(items, data.optInt("total"), data.optInt("page", 1))
    }

    suspend fun updateUserStatus(id: Int, status: String): String {
        val response = ApiClient.post(
            "admin-pengguna-status",
            JSONObject().apply {
                put("id", id)
                put("status", status)
            },
            token,
        )
        return response.message
    }

    suspend fun destinations(query: String = "", status: String = ""): List<Destination> {
        val params = buildMap {
            if (query.isNotBlank()) put("q", query)
            if (status.isNotBlank()) put("status", status)
        }
        val response = ApiClient.get("admin-destinasi", params, token)
        val array = response.data as? JSONArray ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                array.optJSONObject(i)?.let { add(Destination.fromJson(it)) }
            }
        }
    }

    suspend fun uploadDestinationImage(file: java.io.File): String {
        val response = ApiClient.upload(
            action = "upload-gambar",
            file = file,
            fieldName = "gambar",
            fields = mapOf("konteks" to "destinasi"),
            token = token,
        )
        return (response.data as? JSONObject)?.optString("url").orEmpty()
    }

    suspend fun saveDestination(form: DestinationForm): Destination {
        val body = JSONObject().apply {
            if (form.id > 0) put("id", form.id)
            put("nama", form.name.trim())
            put("kategori", form.category)
            put("area", form.area.trim())
            put("deskripsi_singkat", form.shortDescription.trim())
            put("deskripsi", form.description.trim())
            put("gambar", form.imageUrl.trim())
            put("harga_tiket", form.ticketPrice)
            put("jam_buka", form.openHours.trim())
            put("latitude", form.latitude)
            put("longitude", form.longitude)
            put("status", if (form.published) "terbit" else "draf")
            put("unggulan", if (form.featured) 1 else 0)
        }
        val response = ApiClient.post("admin-destinasi-simpan", body, token)
        return Destination.fromJson(response.data as? JSONObject ?: JSONObject())
    }

    suspend fun deleteDestination(id: Int): String {
        val response = ApiClient.post("admin-destinasi-hapus", JSONObject().put("id", id), token)
        return response.message
    }
}
