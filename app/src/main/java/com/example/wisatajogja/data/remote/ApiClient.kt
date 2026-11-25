package com.example.wisatajogja.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.URLEncoder
import java.net.UnknownHostException
import java.util.UUID

data class ApiResponse(
    val message: String,
    val data: Any?,
)

object ApiClient {
    const val BASE_URL = "https://45.127.35.252/wisatajogja/api/index.php"

    suspend fun get(
        action: String,
        params: Map<String, String> = emptyMap(),
        token: String? = null,
    ): ApiResponse = execute(
        action = action,
        method = "GET",
        params = params,
        body = null,
        token = token,
    )

    suspend fun post(
        action: String,
        body: JSONObject = JSONObject(),
        token: String? = null,
    ): ApiResponse = execute(
        action = action,
        method = "POST",
        body = body,
        token = token,
    )

    suspend fun upload(
        action: String,
        file: File,
        fieldName: String = "gambar",
        fields: Map<String, String> = emptyMap(),
        token: String?,
    ): ApiResponse = withContext(Dispatchers.IO) {
        val boundary = "WJ-${UUID.randomUUID()}"
        val connection = (URL(buildUrl(action, emptyMap())).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 60_000
            doOutput = true
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
        }
        try {
            connection.outputStream.buffered().use { output ->
                fun writeText(value: String) = output.write(value.toByteArray(Charsets.UTF_8))

                fields.forEach { (key, value) ->
                    writeText("--$boundary\r\n")
                    writeText("Content-Disposition: form-data; name=\"$key\"\r\n\r\n")
                    writeText("$value\r\n")
                }
                writeText("--$boundary\r\n")
                writeText("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"${file.name}\"\r\n")
                writeText("Content-Type: image/jpeg\r\n\r\n")
                file.inputStream().use { input -> input.copyTo(output) }
                writeText("\r\n--$boundary--\r\n")
            }
            parse(connection)
        } catch (error: SocketTimeoutException) {
            throw ApiException("Koneksi ke server timeout. Coba lagi.")
        } catch (error: UnknownHostException) {
            throw ApiException("Server tidak dapat dijangkau. Periksa koneksi internet.")
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun execute(
        action: String,
        method: String,
        params: Map<String, String> = emptyMap(),
        body: JSONObject? = null,
        token: String? = null,
    ): ApiResponse = withContext(Dispatchers.IO) {
        val connection = (URL(buildUrl(action, params)).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 20_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            if (!token.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            if (body != null) {
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            }
            parse(connection)
        } catch (error: SocketTimeoutException) {
            throw ApiException("Koneksi ke server timeout. Coba lagi.")
        } catch (error: UnknownHostException) {
            throw ApiException("Server tidak dapat dijangkau. Periksa koneksi internet.")
        } catch (error: ApiException) {
            throw error
        } catch (error: Exception) {
            throw ApiException("Gagal menghubungi server: ${error.message ?: "kesalahan jaringan"}")
        } finally {
            connection.disconnect()
        }
    }

    private fun buildUrl(action: String, params: Map<String, String>): String {
        val query = buildString {
            append("action=").append(URLEncoder.encode(action, "UTF-8"))
            params.forEach { (key, value) ->
                append('&').append(URLEncoder.encode(key, "UTF-8"))
                    .append('=').append(URLEncoder.encode(value, "UTF-8"))
            }
        }
        return "$BASE_URL?$query"
    }

    private fun parse(connection: HttpURLConnection): ApiResponse {
        val stream = if (connection.responseCode in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
        val json = runCatching { JSONObject(text) }.getOrNull()
        val message = json?.optString("message").orEmpty()
        if (connection.responseCode !in 200..299 || json?.optBoolean("success") != true) {
            val fallback = when (connection.responseCode) {
                401 -> "Sesi berakhir. Silakan masuk kembali."
                403 -> "Anda tidak memiliki akses."
                404 -> "Data tidak ditemukan."
                else -> "Permintaan gagal (${connection.responseCode})."
            }
            throw ApiException(message.ifBlank { fallback }, connection.responseCode)
        }
        val rawData = json.opt("data")
        val data: Any? = when (rawData) {
            null, JSONObject.NULL -> null
            is JSONArray -> rawData
            is JSONObject -> rawData
            else -> rawData
        }
        return ApiResponse(message, data)
    }
}
