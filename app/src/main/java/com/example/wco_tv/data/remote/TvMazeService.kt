package com.example.wco_tv.data.remote

import com.example.wco_tv.data.local.CacheManager
import com.example.wco_tv.data.local.CacheManager.Artwork
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object TvMazeRepository {
    
    private var cacheManager: CacheManager? = null
    // Cache maps Title -> Artwork object
    private val artworkCache = mutableMapOf<String, Artwork?>()

    fun initialize(manager: CacheManager) {
        this.cacheManager = manager
        artworkCache.putAll(manager.getArtworkMapping())
        Log.d("TvMaze", "Initialized with ${artworkCache.size} cached artwork mappings")
    }

    fun getCachedArtwork(title: String): Artwork? {
        val clean = cleanTitle(title)
        return artworkCache[title] ?: artworkCache[clean]
    }

    suspend fun searchShow(rawTitle: String): Artwork? {
        // 1. Check in-memory cache
        if (artworkCache.containsKey(rawTitle)) return artworkCache[rawTitle]

        // 2. Clean the title
        val cleanTitle = cleanTitle(rawTitle)
        
        // 3. Check in-memory again with clean title
        if (artworkCache.containsKey(cleanTitle)) return artworkCache[cleanTitle]

        if (cleanTitle.length < 2) {
            updateCache(rawTitle, null)
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                // Use 'singlesearch' with 'embed=images' to get everything in one request
                val query = URLEncoder.encode(cleanTitle, "UTF-8")
                val url = URL("https://api.tvmaze.com/singlesearch/shows?q=$query&embed=images")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val showObj = JSONObject(response)
                    
                    // Extract Poster
                    val imageObj = showObj.optJSONObject("image")
                    val posterUrl = imageObj?.optString("original") 
                        ?: imageObj?.optString("medium")
                    
                    // Extract Background
                    var backgroundUrl: String? = null
                    val embedded = showObj.optJSONObject("_embedded")
                    val imagesArray = embedded?.optJSONArray("images")
                    
                    if (imagesArray != null) {
                        for (i in 0 until imagesArray.length()) {
                            val img = imagesArray.getJSONObject(i)
                            if (img.optString("type") == "background") {
                                val resolutions = img.optJSONObject("resolutions")
                                backgroundUrl = resolutions?.optJSONObject("original")?.optString("url")
                                if (backgroundUrl != null) break // Found best one
                            }
                        }
                    }

                    val artwork = Artwork(posterUrl, backgroundUrl)
                    
                    updateCache(rawTitle, artwork)
                    if (rawTitle != cleanTitle) {
                        updateCache(cleanTitle, artwork)
                    }
                    
                    Log.d("TvMaze", "Found artwork for '$cleanTitle': Poster=${posterUrl != null}, BG=${backgroundUrl != null}")
                    artwork
                } else {
                    Log.d("TvMaze", "No match or error for '$cleanTitle': ${connection.responseCode}")
                    updateCache(rawTitle, null)
                    null
                }
            } catch (e: Exception) {
                Log.e("TvMaze", "Exception searching TVMaze", e)
                null
            }
        }
    }

    private fun updateCache(key: String, value: Artwork?) {
        artworkCache[key] = value
        cacheManager?.saveArtworkMapping(artworkCache)
    }

    private fun cleanTitle(title: String): String {
        // Regex to remove common WCO suffixes/prefixes
        var cleaned = title
            .replace(Regex("(?i)Season\\s+\\d+.*"), "") // Remove "Season 1..."
            .replace(Regex("(?i)Episode\\s+\\d+.*"), "") // Remove "Episode 1..."
            .replace(Regex("(?i)English\\s+Dubbed"), "")
            .replace(Regex("(?i)Dubbed"), "")
            .replace(Regex("(?i)Subbed"), "")
            .replace(Regex("(?i)OVA"), "")
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ") // Remove special chars
            .trim()
        
        // If we stripped too much, revert to original (or handle edge cases)
        if (cleaned.isBlank()) return title
        return cleaned
    }
}
