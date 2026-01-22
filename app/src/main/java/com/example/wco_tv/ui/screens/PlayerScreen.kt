package com.example.wco_tv.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.wco_tv.WORKING_USER_AGENT
import com.example.wco_tv.data.local.CacheManager
import com.example.wco_tv.data.model.VideoQuality
import kotlinx.coroutines.delay

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoUrl: String,
    qualities: List<VideoQuality> = emptyList(),
    episodeUrl: String,
    cacheManager: CacheManager,
    nextEpisodeTitle: String? = null,
    onPlayNext: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentUrl by remember { mutableStateOf(videoUrl) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEnded by remember { mutableStateOf(false) }

    Log.d("WCO_TV_PLAYER", "Initializing Player with URL: $currentUrl")

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Base headers
    val baseHeaders = remember {
        mapOf(
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.5",
            "Sec-GPC" to "1",
            "Connection" to "keep-alive",
            "Upgrade-Insecure-Requests" to "1",
            "DNT" to "1",
            "Pragma" to "no-cache",
            "Cache-Control" to "no-cache"
        )
    }

    // Load media when URL changes
    LaunchedEffect(currentUrl) {
        val currentPos = exoPlayer.currentPosition
        
        // Find headers for this quality
        val qualityHeaders = qualities.find { it.url == currentUrl }?.headers ?: emptyMap()
        val allHeaders = baseHeaders + qualityHeaders
        
        Log.d("WCO_TV_PLAYER", "Playing with headers: $allHeaders")

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(WORKING_USER_AGENT)
            .setDefaultRequestProperties(allHeaders)
            
        val mediaSource = DefaultMediaSourceFactory(dataSourceFactory)
            .createMediaSource(MediaItem.fromUri(currentUrl))

        exoPlayer.setMediaSource(mediaSource)
        
        if (currentPos > 0) {
             exoPlayer.seekTo(currentPos)
        } else {
            // Restore progress only on initial load (when pos is 0)
            val savedInfo = cacheManager.getPlaybackProgress(episodeUrl)
            if (savedInfo != null && savedInfo.position > 0) {
                Log.d("WCO_TV_PLAYER", "Restoring playback position: ${savedInfo.position} ms")
                exoPlayer.seekTo(savedInfo.position)
            }
        }
        
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
        isEnded = false
    }

    // Player Listeners
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Log.e("WCO_TV_PLAYER", "ExoPlayer Error: ${error.message}", error)
                errorMessage = "Playback Error: ${error.errorCodeName}\n${error.message}"
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED && !isEnded) {
                    isEnded = true
                    // Clear progress on finish so it restarts next time
                    cacheManager.savePlaybackProgress(episodeUrl, 0, exoPlayer.duration)
                    if (nextEpisodeTitle != null) {
                        onPlayNext()
                    }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            // Save progress on exit
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            if (!isEnded && pos > 1000 && dur > 0) {
                 cacheManager.savePlaybackProgress(episodeUrl, pos, dur)
            }
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Progress Polling for saving state
    LaunchedEffect(exoPlayer, isEnded) {
        while (!isEnded) {
            val duration = exoPlayer.duration
            val position = exoPlayer.currentPosition
            
            if (duration > 0) {
                // Save progress periodically (every 5s)
                if (position > 5000 && (duration - position) > 10000) { // Don't save if just started or nearly finished
                     cacheManager.savePlaybackProgress(episodeUrl, position, duration)
                }
            }
            delay(1000)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false
                        keepScreenOn = true
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}