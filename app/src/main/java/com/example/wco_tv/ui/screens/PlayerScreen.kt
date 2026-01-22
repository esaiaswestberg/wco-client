package com.example.wco_tv.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.example.wco_tv.CinematicAccent
import com.example.wco_tv.CinematicSurface
import com.example.wco_tv.WORKING_USER_AGENT
import com.example.wco_tv.data.local.CacheManager
import com.example.wco_tv.data.model.VideoQuality
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    videoUrl: String,
    qualities: List<VideoQuality> = emptyList(),
    episodeUrl: String,
    cacheManager: CacheManager,
    nextEpisodeTitle: String? = null,
    onPlayNext: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentUrl by remember { mutableStateOf(videoUrl) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var timeLeft by remember { mutableStateOf<Long?>(null) }
    var isEnded by remember { mutableStateOf(false) }
    val nextButtonFocusRequester = remember { FocusRequester() }
    
    // Quality selection state
    var showQualitySelector by remember { mutableStateOf(false) }
    val qualityButtonFocusRequester = remember { FocusRequester() }

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
        timeLeft = null
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

    // Progress Polling for "Next Episode" button and saving state
    LaunchedEffect(exoPlayer, isEnded) {
        while (!isEnded) {
            val duration = exoPlayer.duration
            val position = exoPlayer.currentPosition
            
            if (duration > 0) {
                timeLeft = duration - position
                
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
                        useController = true
                        keepScreenOn = true
                        setShowNextButton(false) // Hide default next button
                        
                        // Custom controller visibility listener could go here
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Quality Selector
            if (qualities.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    if (showQualitySelector) {
                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            qualities.forEach { quality ->
                                Button(
                                    onClick = {
                                        currentUrl = quality.url
                                        showQualitySelector = false
                                    },
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                    colors = ButtonDefaults.colors(
                                        containerColor = if (currentUrl == quality.url) CinematicAccent else Color.Gray
                                    )
                                ) {
                                    Text(quality.label)
                                }
                            }
                        }
                    } else {
                         Button(
                            onClick = { showQualitySelector = true },
                            colors = ButtonDefaults.colors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            modifier = Modifier.focusRequester(qualityButtonFocusRequester)
                        ) {
                            Text("Quality")
                        }
                    }
                }
            }
            
            // "Next Episode" Overlay
            if (nextEpisodeTitle != null && timeLeft != null && timeLeft!! < 30000 && !isEnded) {
                // Auto-focus the button when it appears
                LaunchedEffect(Unit) {
                    nextButtonFocusRequester.requestFocus()
                }

                Button(
                    onClick = onPlayNext,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 80.dp, end = 40.dp) // Position above timeline
                        .focusRequester(nextButtonFocusRequester),
                    colors = ButtonDefaults.colors(
                        containerColor = CinematicSurface.copy(alpha = 0.9f),
                        focusedContainerColor = CinematicAccent
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                ) {
                    Row(modifier = Modifier.padding(12.dp)) {
                        Text("Next: $nextEpisodeTitle", color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("(${timeLeft!! / 1000}s)", color = Color.Gray)
                    }
                }
            }
        }
    }
}
