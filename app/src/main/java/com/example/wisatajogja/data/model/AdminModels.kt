package com.example.wisatajogja.data.model

import org.json.JSONObject

data class AdminMetrics(
    val visits30: Int,
    val visitsPercent: Double,
    val totalUsers: Int,
    val newUsers: Int,
    val totalDestinations: Int,
    val publishedDestinations: Int,
    val draftDestinations: Int,
    val totalSaved: Int,
    val totalReviews: Int,
    val averageRating: Double,
    val totalReservations: Int,
)

data class ChartPoint(
    val date: String,
    val label: String,
    val visits: Int,
)

data class AdminUser(
    val id: Int,
    val name: String,
    val username: String,
    val email: String,
    val role: String,
    val status: String,
    val photoUrl: String?,
    val createdAt: String,
    val savedCount: Int,
    val reservationCount: Int,
) {
    companion object {
        fun fromJson(json: JSONObject): AdminUser = AdminUser(
            id = json.optInt("id"),
            name = json.optString("nama"),
            username = json.optString("username"),
            email = json.optString("email"),
            role = json.optString("role", "user"),
            status = json.optString("status", "aktif"),
            photoUrl = json.stringOrNull("foto_profil"),
            createdAt = json.optString("created_at"),
            savedCount = json.optInt("jumlah_tersimpan"),
            reservationCount = json.optInt("jumlah_reservasi"),
        )
    }
}

data class AdminUserPage(
    val items: List<AdminUser>,
    val total: Int,
    val page: Int,
)

data class PopularDestination(
    val id: Int,
    val name: String,
    val area: String,
    val imageUrl: String?,
    val rating: Double,
    val savedCount: Int,
)

data class RecentReservation(
    val code: String,
    val userName: String,
    val destinationName: String,
    val visitDate: String,
    val people: Int,
    val status: String,
)

data class AdminSummary(
    val metrics: AdminMetrics,
    val chart: List<ChartPoint>,
    val latestUsers: List<AdminUser>,
    val popularDestinations: List<PopularDestination>,
    val latestReservations: List<RecentReservation>,
) {
    companion object {
        fun fromJson(json: JSONObject): AdminSummary {
            val metricsJson = json.optJSONObject("metrik") ?: JSONObject()
            val chartJson = json.optJSONArray("grafik")
            val usersJson = json.optJSONArray("pengguna_terbaru")
            val popularJson = json.optJSONArray("destinasi_populer")
            val reservationsJson = json.optJSONArray("reservasi_terbaru")
            return AdminSummary(
                metrics = AdminMetrics(
                    visits30 = metricsJson.optInt("kunjungan_30"),
                    visitsPercent = metricsJson.optDouble("kunjungan_persen", 0.0),
                    totalUsers = metricsJson.optInt("total_pengguna"),
                    newUsers = metricsJson.optInt("pengguna_baru"),
                    totalDestinations = metricsJson.optInt("total_destinasi"),
                    publishedDestinations = metricsJson.optInt("destinasi_terbit"),
                    draftDestinations = metricsJson.optInt("destinasi_draf"),
                    totalSaved = metricsJson.optInt("total_tersimpan"),
                    totalReviews = metricsJson.optInt("total_ulasan"),
                    averageRating = metricsJson.optDouble("rating_rata", 0.0),
                    totalReservations = metricsJson.optInt("total_reservasi"),
                ),
                chart = buildList {
                    if (chartJson != null) for (i in 0 until chartJson.length()) {
                        val item = chartJson.optJSONObject(i) ?: continue
                        add(ChartPoint(item.optString("tanggal"), item.optString("label"), item.optInt("kunjungan")))
                    }
                },
                latestUsers = buildList {
                    if (usersJson != null) for (i in 0 until usersJson.length()) {
                        usersJson.optJSONObject(i)?.let { add(AdminUser.fromJson(it)) }
                    }
                },
                popularDestinations = buildList {
                    if (popularJson != null) for (i in 0 until popularJson.length()) {
                        val item = popularJson.optJSONObject(i) ?: continue
                        add(
                            PopularDestination(
                                id = item.optInt("id"),
                                name = item.optString("nama"),
                                area = item.optString("area"),
                                imageUrl = item.stringOrNull("gambar"),
                                rating = item.optDouble("rating", 0.0),
                                savedCount = item.optInt("tersimpan"),
                            )
                        )
                    }
                },
                latestReservations = buildList {
                    if (reservationsJson != null) for (i in 0 until reservationsJson.length()) {
                        val item = reservationsJson.optJSONObject(i) ?: continue
                        add(
                            RecentReservation(
                                code = item.optString("kode_booking"),
                                userName = item.optString("pengguna"),
                                destinationName = item.optString("destinasi"),
                                visitDate = item.optString("tanggal_kunjungan"),
                                people = item.optInt("jumlah_orang"),
                                status = item.optString("status"),
                            )
                        )
                    }
                },
            )
        }
    }
}
