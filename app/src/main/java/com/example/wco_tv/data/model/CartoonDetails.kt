package com.example.wco_tv.data.model

data class CartoonDetails(
    val title: String,
    val description: String,
    val imageUrl: String,
    val genres: List<String>,
    val episodes: List<Episode> = emptyList(),
    val seasonNames: Map<String, String> = emptyMap()
)

data class Episode(
    val title: String,
    val url: String,
    val seasonId: String = "s1" // Default to season 1 if not found
)
