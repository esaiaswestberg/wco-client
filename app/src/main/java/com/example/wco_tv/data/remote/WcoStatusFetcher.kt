package com.example.wco_tv.data.remote

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WcoStatusFetcher {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun fetchAvailableDomains(): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url("https://www.wcostatus.com/check.php")
                    .header("User-Agent", "Mozilla/5.0") // Simple UA
                    .header("Accept", "application/json")
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("WcoStatusFetcher", "Failed to fetch status: ${response.code}")
                        return@withContext emptyList()
                    }

                    val jsonString = response.body?.string() ?: return@withContext emptyList()
                    val jsonArray = JSONArray(jsonString)
                    val validDomains = mutableListOf<String>()

                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val status = obj.optInt("status")
                        val domain = obj.optString("domain")
                        
                        // We strictly want 200 OK and a valid http/https string
                        if (status == 200 && domain.startsWith("http")) {
                            // The API returns escaped slashes like "https:\/\/www.wco.tv", 
                            // but JSONObject.getString usually handles that. 
                            // If manually parsing, we'd need to unescape, but org.json should handle standard JSON escapes.
                            // However, let's ensure it's clean.
                            validDomains.add(domain.replace("\\/", "/")) 
                        }
                    }
                    Log.d("WcoStatusFetcher", "Found ${validDomains.size} valid domains")
                    validDomains
                }
            } catch (e: Exception) {
                Log.e("WcoStatusFetcher", "Error fetching domains", e)
                emptyList()
            }
        }
    }
}
