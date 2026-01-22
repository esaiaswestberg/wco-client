package com.example.wco_tv.data.remote

import android.util.Log
import com.example.wco_tv.data.model.VideoQuality
import com.example.wco_tv.WORKING_USER_AGENT
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import java.net.URLDecoder

object WcoVideoFetcher {
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun fetchVideoQualities(episodeUrl: String, baseUrl: String): List<VideoQuality> {
        return try {
            Log.d("WcoVideoFetcher", "Fetching episode page: $episodeUrl")
            // Step 1: Get Episode Page
            val episodeHtml = makeRequest(episodeUrl)
            val iframeUrl = parseIframeUrl(episodeHtml) ?: throw Exception("Iframe URL not found")
            Log.d("WcoVideoFetcher", "Found Iframe URL: $iframeUrl")

            // Step 2: Get Iframe Page
            val iframeHtml = makeRequest(iframeUrl, referer = "$baseUrl/") 
            val getVidLinkPath = parseGetVidLink(iframeHtml) ?: throw Exception("getvidlink API URL not found")
            
            val iframeUri = java.net.URI(iframeUrl)
            val getVidLinkUrl = "https://${iframeUri.host}$getVidLinkPath"
            Log.d("WcoVideoFetcher", "Found API URL: $getVidLinkUrl")

            // Step 3: Get Video JSON Tokens
            val jsonResponse = makeRequest(
                getVidLinkUrl, 
                referer = iframeUrl, 
                headers = mapOf("X-Requested-With" to "XMLHttpRequest")
            )
            Log.d("WcoVideoFetcher", "JSON Response: $jsonResponse")
            
            val json = JSONObject(jsonResponse)
            val server = json.getString("server")
            val enc = json.optString("enc")
            val hd = json.optString("hd")
            val fhd = json.optString("fhd")

            val qualities = mutableListOf<VideoQuality>()
            val videoHeaders = mapOf("Referer" to "https://${java.net.URI(iframeUrl).host}/")

            // Step 4: Get Redirect URLs
            if (fhd.isNotEmpty()) {
                val finalUrl = getRedirectUrl(server, fhd, iframeUrl)
                if (finalUrl != null) qualities.add(VideoQuality("1080p", finalUrl, videoHeaders))
            }
            if (hd.isNotEmpty()) {
                val finalUrl = getRedirectUrl(server, hd, iframeUrl)
                if (finalUrl != null) qualities.add(VideoQuality("720p", finalUrl, videoHeaders))
            }
            if (enc.isNotEmpty()) {
                val finalUrl = getRedirectUrl(server, enc, iframeUrl)
                if (finalUrl != null) qualities.add(VideoQuality("SD", finalUrl, videoHeaders))
            }

            qualities
        } catch (e: Exception) {
            Log.e("WcoVideoFetcher", "Error fetching videos", e)
            emptyList()
        }
    }

    private fun makeRequest(url: String, referer: String? = null, headers: Map<String, String> = emptyMap()): String {
        val requestBuilder = Request.Builder()
            .url(url)
            .header("User-Agent", WORKING_USER_AGENT)
        
        if (referer != null) {
            requestBuilder.header("Referer", referer)
        }
        
        headers.forEach { (k, v) -> requestBuilder.header(k, v) }

        client.newCall(requestBuilder.build()).execute().use { response ->
            if (!response.isSuccessful) throw Exception("Request failed: ${response.code}")
            return response.body?.string() ?: ""
        }
    }

    private fun getRedirectUrl(server: String, token: String, referer: String): String? {
        val url = "$server/getvid?evid=$token&json"
        try {
            val jsonResponse = makeRequest(
                url,
                referer = referer,
                headers = mapOf("X-Requested-With" to "XMLHttpRequest")
            )
            // The response is usually a JSON string "https://..." or object.
            // Based on docs: ` "https:\/\/t01.wcostream.com\/..." `
            // It seems the response IS the URL string directly, or a JSON string containing it.
            // Let's check if it's a valid JSON string first.
            
            // Sometimes it returns a simple string wrapped in quotes if it's just the URL.
            // Or it could be { "cdn": "...", ... }?
            // The docs say:
            // Response: "https:\/\/t01.wcostream.com\/getvid?evid=...&json"
            // This looks like a JSON string.
            
            // Let's try to parse as JSON string if it starts with ".
            if (jsonResponse.trim().startsWith("\"")) {
                 return JSONObject("{\"url\": $jsonResponse}").getString("url")
            }
            // Fallback: maybe it's just the raw string?
            return jsonResponse
        } catch (e: Exception) {
            Log.e("WcoVideoFetcher", "Error getting redirect URL", e)
            return null
        }
    }

    private fun parseIframeUrl(html: String): String? {
        val doc = Jsoup.parse(html)
        return doc.select("div.iframe-16x9 iframe").attr("src").ifEmpty {
            doc.select("iframe#cizgi-js-0").attr("src")
        }.let { 
            if (it.isNotEmpty() && !it.startsWith("http")) "https:$it" else it.ifEmpty { null }
        }
    }

    private fun parseGetVidLink(html: String): String? {
        // Search for $.getJSON("/inc/embed/getvidlink.php?...")
        val regex = """\$\.getJSON\("([^"]+)""".toRegex()
        val match = regex.find(html)
        return match?.groupValues?.get(1)
    }
}
