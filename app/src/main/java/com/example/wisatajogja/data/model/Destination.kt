package com.example.wisatajogja.data.model

import org.json.JSONObject

data class GalleryItem(
    val id: Int,
    val imageUrl: String,
)

data class DestinationService(
    val id: Int,
    val type: String,
    val name: String,
    val description: String,
    val price: Int,
    val icon: String,
) {
    val isCommunity: Boolean get() = type == "komunitas"
}

data class MyReview(
    val rating: Int,
    val comment: String,
    val date: String,
)

data class Review(
    val authorName: String,
    val authorPhotoUrl: String?,
    val rating: Int,
    val comment: String,
    val date: String,
)

data class Destination(
    val id: Int,
    val name: String,
    val slug: String,
    val category: String,
    val area: String,
    val shortDescription: String,
    val description: String,
    val imageUrl: String?,
    val ticketPrice: Int,
    val openHours: String,
    val rating: Double,
    val reviewCount: Int,
    val viewCount: Int,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val featured: Boolean,
    val isFavorite: Boolean,
    val savedCount: Int = 0,
    val gallery: List<GalleryItem> = emptyList(),
    val services: List<DestinationService> = emptyList(),
    val reviews: List<Review> = emptyList(),
    val myReview: MyReview? = null,
    val similar: List<Destination> = emptyList(),
) {
    companion object {
        fun fromJson(json: JSONObject): Destination {
            val galleryJson = json.optJSONArray("galeri")
            val gallery = buildList {
                if (galleryJson != null) {
                    for (i in 0 until galleryJson.length()) {
                        val item = galleryJson.optJSONObject(i) ?: continue
                        add(GalleryItem(item.optInt("id"), item.optString("gambar")))
                    }
                }
            }
            val servicesJson = json.optJSONArray("layanan")
            val services = buildList {
                if (servicesJson != null) {
                    for (i in 0 until servicesJson.length()) {
                        val item = servicesJson.optJSONObject(i) ?: continue
                        add(
                            DestinationService(
                                id = item.optInt("id"),
                                type = item.optString("tipe"),
                                name = item.optString("nama"),
                                description = item.optString("deskripsi"),
                                price = item.optInt("harga"),
                                icon = item.optString("ikon"),
                            )
                        )
                    }
                }
            }
            val reviewsJson = json.optJSONArray("ulasan")
            val reviews = buildList {
                if (reviewsJson != null) {
                    for (i in 0 until reviewsJson.length()) {
                        val item = reviewsJson.optJSONObject(i) ?: continue
                        add(
                            Review(
                                authorName = item.optString("nama"),
                                authorPhotoUrl = item.stringOrNull("foto_profil"),
                                rating = item.optInt("rating"),
                                comment = item.optString("komentar"),
                                date = item.optString("tanggal"),
                            )
                        )
                    }
                }
            }
            val myReview = json.optJSONObject("ulasan_saya")?.let {
                MyReview(
                    rating = it.optInt("rating"),
                    comment = it.optString("komentar"),
                    date = it.optString("tanggal"),
                )
            }
            val similarJson = json.optJSONArray("mirip")
            val similar = buildList {
                if (similarJson != null) {
                    for (i in 0 until similarJson.length()) {
                        similarJson.optJSONObject(i)?.let { add(fromJson(it)) }
                    }
                }
            }
            return Destination(
                id = json.optInt("id"),
                name = json.optString("nama"),
                slug = json.optString("slug"),
                category = json.optString("kategori"),
                area = json.optString("area"),
                shortDescription = json.optString("deskripsi_singkat"),
                description = json.optString("deskripsi"),
                imageUrl = json.stringOrNull("gambar"),
                ticketPrice = json.optInt("harga_tiket"),
                openHours = json.optString("jam_buka"),
                rating = json.optDouble("rating", 0.0),
                reviewCount = json.optInt("jumlah_ulasan"),
                viewCount = json.optInt("jumlah_dilihat"),
                latitude = json.optDouble("latitude", 0.0),
                longitude = json.optDouble("longitude", 0.0),
                status = json.optString("status", "terbit"),
                featured = json.optBoolean("unggulan"),
                isFavorite = json.optBoolean("favorit"),
                savedCount = json.optInt("tersimpan"),
                gallery = gallery,
                services = services,
                reviews = reviews,
                myReview = myReview,
                similar = similar,
            )
        }
    }
}

data class Reservation(
    val id: Int,
    val code: String,
    val destinationName: String,
    val area: String,
    val imageUrl: String?,
    val visitDate: String,
    val people: Int,
    val totalPrice: Int,
    val notes: String,
    val status: String,
) {
    companion object {
        fun fromJson(json: JSONObject): Reservation = Reservation(
            id = json.optInt("id"),
            code = json.optString("kode_booking"),
            destinationName = json.optString("destinasi"),
            area = json.optString("area"),
            imageUrl = json.stringOrNull("gambar"),
            visitDate = json.optString("tanggal_kunjungan"),
            people = json.optInt("jumlah_orang"),
            totalPrice = json.optInt("total_harga"),
            notes = json.optString("catatan"),
            status = json.optString("status"),
        )
    }
}
