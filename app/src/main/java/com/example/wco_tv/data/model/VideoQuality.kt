package com.example.wco_tv.data.model

data class VideoQuality(
    val label: String,
    val url: String,
    val headers: Map<String, String> = emptyMap()
)
