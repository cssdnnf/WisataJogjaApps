package com.example.wisatajogja.data.weather

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WeatherRepository {
    @Volatile
    private var cachedAt: Long = 0L

    @Volatile
    private var cached: Weather? = null

    suspend fun load(force: Boolean = false): Weather {
        val now = System.currentTimeMillis()
        val existing = cached
        if (!force && existing != null && now - cachedAt < CACHE_MS) return existing

        val weather = withContext(Dispatchers.IO) {
            val bmkg = runCatching { fetchBmkg() }.getOrNull()
            val meteo = runCatching { fetchOpenMeteo() }.getOrNull()
            when {
                bmkg != null -> bmkg.copy(
                    uvIndex = meteo?.uvIndex ?: 0.0,
                    source = "BMKG",
                )
                meteo != null -> meteo.copy(source = "Open-Meteo")
                else -> error("Cuaca tidak tersedia.")
            }
        }
        cached = weather
        cachedAt = now
        return weather
    }

    private fun fetchBmkg(): Weather {
        val json = getJson("https://api.bmkg.go.id/publik/prakiraan-cuaca?adm4=34.71.01.1001")
        val data = json.optJSONArray("data") ?: error("Data BMKG kosong")
        val firstLocation = data.optJSONObject(0) ?: error("Lokasi BMKG tidak ditemukan")
        val groups = firstLocation.optJSONArray("cuaca") ?: error("Prakiraan BMKG kosong")
        val forecasts = mutableListOf<JSONObject>()
        for (i in 0 until groups.length()) {
            val group = groups.optJSONArray(i) ?: continue
            for (j in 0 until group.length()) {
                group.optJSONObject(j)?.let { forecasts.add(it) }
            }
        }
        if (forecasts.isEmpty()) error("Prakiraan BMKG tidak tersedia")
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val now = Date()
        val closest = forecasts.minByOrNull { forecast ->
            val time = runCatching { formatter.parse(forecast.optString("local_datetime")) }.getOrNull()
            time?.let { kotlin.math.abs(it.time - now.time) } ?: Long.MAX_VALUE
        } ?: forecasts.first()
        return Weather(
            temperature = closest.optDouble("t", 0.0).toInt(),
            description = closest.optString("weather_desc", "Cerah berawan"),
            humidity = closest.optDouble("hu", 0.0).toInt(),
            windSpeed = closest.optDouble("ws", 0.0),
            uvIndex = 0.0,
            source = "BMKG",
            updatedAt = SimpleDateFormat("HH.mm", Locale("id", "ID")).format(now),
        )
    }

    private fun fetchOpenMeteo(): Weather {
        val json = getJson(
            "https://api.open-meteo.com/v1/forecast?latitude=-7.7956&longitude=110.3695" +
                "&current=temperature_2m,relative_humidity_2m,wind_speed_10m,weather_code,uv_index" +
                "&timezone=Asia%2FJakarta"
        )
        val current = json.optJSONObject("current") ?: error("Data cuaca kosong")
        val code = current.optInt("weather_code")
        return Weather(
            temperature = current.optDouble("temperature_2m").toInt(),
            description = descriptionForCode(code),
            humidity = current.optDouble("relative_humidity_2m").toInt(),
            windSpeed = current.optDouble("wind_speed_10m"),
            uvIndex = current.optDouble("uv_index"),
            source = "Open-Meteo",
            updatedAt = SimpleDateFormat("HH.mm", Locale("id", "ID")).format(Date()),
        )
    }

    private fun getJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12_000
            readTimeout = 18_000
            setRequestProperty("Accept", "application/json")
        }
        return try {
            if (connection.responseCode !in 200..299) error("Cuaca HTTP ${connection.responseCode}")
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
    }

    private fun descriptionForCode(code: Int): String = when (code) {
        0 -> "Cerah"
        1 -> "Cerah berawan"
        2 -> "Berawan sebagian"
        3 -> "Berawan"
        45, 48 -> "Berkabut"
        51, 53, 55, 56, 57 -> "Gerimis"
        61, 63, 65, 66, 67 -> "Hujan"
        71, 73, 75, 77 -> "Hujan salju"
        80, 81, 82 -> "Hujan lokal"
        85, 86 -> "Hujan salju lokal"
        95, 96, 99 -> "Hujan petir"
        else -> "Cerah berawan"
    }

    companion object {
        private const val CACHE_MS = 15 * 60 * 1000L
    }
}
