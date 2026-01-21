package com.example.wco_tv.data.local

import android.content.Context
import com.example.wco_tv.data.model.Cartoon
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import java.io.File

data class CachedData<T>(
    val timestamp: Long,
    val data: T
)

class CacheManager(private val context: Context) {
    private val gson = Gson()
    private val CACHE_DURATION_MS = 12 * 60 * 60 * 1000 // 12 hours

    fun saveCartoons(cartoons: List<Cartoon>) {
        val cache = CachedData(System.currentTimeMillis(), cartoons)
        val json = gson.toJson(cache)
        File(context.cacheDir, "cartoons_cache.json").writeText(json)
    }

    fun getCartoons(): List<Cartoon>? {
        val file = File(context.cacheDir, "cartoons_cache.json")
        if (!file.exists()) return null

        try {
            val json = file.readText()
            val type = object : TypeToken<CachedData<List<Cartoon>>>() {}.type
            val cache: CachedData<List<Cartoon>> = gson.fromJson(json, type)

            if (System.currentTimeMillis() - cache.timestamp > CACHE_DURATION_MS) {
                return null // Expired
            }
            return cache.data
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    data class Artwork(
        val posterUrl: String? = null,
        val backgroundUrl: String? = null
    )

    fun saveArtworkMapping(mapping: Map<String, Artwork?>) {
        try {
            val json = gson.toJson(mapping)
            File(context.cacheDir, "artwork_cache.json").writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getArtworkMapping(): Map<String, Artwork?> {
        val file = File(context.cacheDir, "artwork_cache.json")
        if (!file.exists()) return emptyMap()

        try {
            val json = file.readText()
            val result = mutableMapOf<String, Artwork?>()
            
            // Parse as a generic JsonObject first to handle mixed types
            val jsonObject = com.google.gson.JsonParser.parseString(json).asJsonObject
            
            jsonObject.entrySet().forEach { entry ->
                val key = entry.key
                val element = entry.value
                
                if (element.isJsonPrimitive && element.asJsonPrimitive.isString) {
                    // Legacy: It's just a URL string
                    result[key] = Artwork(posterUrl = element.asString, backgroundUrl = null)
                } else if (element.isJsonObject) {
                    // New: It's an Artwork object
                    val artObj = element.asJsonObject
                    // Handle both "poster" (manual JSON) and "posterUrl" (Gson) just in case, 
                    // but we strictly use Gson now so "posterUrl" is expected.
                    // However, to be safe against my previous edit attempts if any ran (unlikely), strictly usage of Gson class structure.
                    val artwork = gson.fromJson(element, Artwork::class.java)
                    result[key] = artwork
                }
            }
            return result
        } catch (e: Exception) {
            e.printStackTrace()
            return emptyMap()
        }
    }

    fun saveFavorites(favorites: Set<String>) {
        try {
            val json = gson.toJson(favorites)
            File(context.cacheDir, "favorites_cache.json").writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getFavorites(): Set<String> {
        val file = File(context.cacheDir, "favorites_cache.json")
        if (!file.exists()) return emptySet()

        return try {
            val json = file.readText()
            val type = object : TypeToken<Set<String>>() {}.type
            gson.fromJson(json, type) ?: emptySet()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // Playback Progress
    data class PlaybackInfo(
        val position: Long,
        val duration: Long
    )

    fun savePlaybackProgress(url: String, position: Long, duration: Long) {
        try {
            val map = getAllPlaybackProgress().toMutableMap()
            map[url] = PlaybackInfo(position, duration)
            val json = gson.toJson(map)
            File(context.cacheDir, "playback_progress.json").writeText(json)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getPlaybackProgress(url: String): PlaybackInfo? {
        return getAllPlaybackProgress()[url]
    }

    fun getAllPlaybackProgress(): Map<String, PlaybackInfo> {
        val file = File(context.cacheDir, "playback_progress.json")
        if (!file.exists()) return emptyMap()

        return try {
            val json = file.readText()
            val type = object : TypeToken<Map<String, PlaybackInfo>>() {}.type
            gson.fromJson(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }
}