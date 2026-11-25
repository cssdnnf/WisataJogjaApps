package com.example.wisatajogja.data.repository

import com.example.wisatajogja.data.model.Destination
import com.example.wisatajogja.data.remote.ApiClient
import com.example.wisatajogja.data.session.SessionManager
import org.json.JSONArray
import org.json.JSONObject

class DestinationRepository {
    suspend fun explore(
        category: String? = null,
        query: String? = null,
        area: String? = null,
        featuredOnly: Boolean = false,
        limit: Int = 30,
    ): List<Destination> {
        val params = buildMap {
            put("limit", limit.toString())
            if (!category.isNullOrBlank() && category != "Semua") put("kategori", category)
            if (!query.isNullOrBlank()) put("q", query)
            if (!area.isNullOrBlank() && area != "Semua") put("area", area)
            if (featuredOnly) put("unggulan", "1")
        }
        val response = ApiClient.get("destinasi", params, SessionManager.token)
        val data = response.data as? JSONObject ?: return emptyList()
        return data.optJSONArray("items").toDestinations()
    }

    suspend fun detail(id: Int? = null, slug: String? = null): Destination {
        val params = if (id != null && id > 0) mapOf("id" to id.toString()) else mapOf("slug" to slug.orEmpty())
        val response = ApiClient.get("destinasi-detail", params, SessionManager.token)
        val json = response.data as? JSONObject ?: error("Detail destinasi tidak tersedia.")
        return Destination.fromJson(json)
    }

    suspend fun savedDestinations(): List<Destination> {
        val response = ApiClient.get("favorit", token = SessionManager.token)
        return (response.data as? JSONArray).toDestinations()
    }

    suspend fun toggleFavorite(destinationId: Int): Boolean {
        val response = ApiClient.post(
            "favorit-toggle",
            JSONObject().put("destinasi_id", destinationId),
            SessionManager.token,
        )
        return (response.data as? JSONObject)?.optBoolean("favorit") == true
    }

    suspend fun submitReview(destinationId: Int, rating: Int, comment: String): JSONObject {
        val response = ApiClient.post(
            "ulasan-buat",
            JSONObject().apply {
                put("destinasi_id", destinationId)
                put("rating", rating)
                put("komentar", comment.trim())
            },
            SessionManager.token,
        )
        return response.data as? JSONObject ?: JSONObject()
    }

    suspend fun areas(): List<Pair<String, Int>> {
        val response = ApiClient.get("area")
        val array = response.data as? JSONArray ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(item.optString("area") to item.optInt("jumlah"))
            }
        }
    }

    private fun JSONArray?.toDestinations(): List<Destination> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                optJSONObject(i)?.let { add(Destination.fromJson(it)) }
            }
        }
    }
}
