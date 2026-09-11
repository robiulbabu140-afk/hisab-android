package com.hisab.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ApiException(message: String, val code: Int = -1) : Exception(message)

/**
 * Thin JSON HTTP client for the Hisab web backend's `api` PHP endpoints (see
 * hisab-web/api/ and hisab-web/DEPLOY.md on the server side). Every call needs
 * [SyncPrefs.baseUrl] and [SyncPrefs.apiKey] to already be set — [SyncManager] checks
 * that before using this.
 */
class ApiClient(private val syncPrefs: SyncPrefs) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun endpoint(path: String) = "${syncPrefs.baseUrl}/api/$path"

    private fun requestBuilder(path: String) = Request.Builder()
        .url(endpoint(path))
        .header("X-Api-Key", syncPrefs.apiKey)

    suspend fun getArray(path: String): JSONArray = withContext(Dispatchers.IO) {
        val request = requestBuilder(path).get().build()
        JSONArray(executeRaw(request))
    }

    suspend fun getObject(path: String): JSONObject = withContext(Dispatchers.IO) {
        val request = requestBuilder(path).get().build()
        JSONObject(executeRaw(request))
    }

    suspend fun postForObject(path: String, body: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val request = requestBuilder(path)
            .post(body.toString().toRequestBody(jsonMediaType))
            .build()
        JSONObject(executeRaw(request))
    }

    /** Runs the call and returns the raw response body, throwing [ApiException] on any non-2xx status. */
    private fun executeRaw(request: Request): String {
        client.newCall(request).execute().use { response ->
            val text = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val serverMessage = runCatching { JSONObject(text).optString("error") }.getOrNull()
                throw ApiException(serverMessage?.takeIf { it.isNotBlank() } ?: "HTTP ${response.code}", response.code)
            }
            return text
        }
    }
}
