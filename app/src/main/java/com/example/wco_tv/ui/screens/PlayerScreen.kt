package com.example.wco_tv.ui.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.wco_tv.CinematicAccent
import com.example.wco_tv.CinematicText
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

    // Player State for UI
    var isPlaying by remember { mutableStateOf(false) }
    var playbackPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }

    Log.d("WCO_TV_PLAYER", "Initializing Player with URL: $currentUrl")

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build()
    }

    // Helper to format time
    fun formatTime(ms: Long): String {
        val totalSeconds = ms / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
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
                isPlaying = exoPlayer.isPlaying
                duration = exoPlayer.duration.coerceAtLeast(0L)
                if (state == Player.STATE_ENDED && !isEnded) {
                    isEnded = true
                    // Clear progress on finish so it restarts next time
                    cacheManager.savePlaybackProgress(episodeUrl, 0, exoPlayer.duration)
                    if (nextEpisodeTitle != null) {
                        onPlayNext()
                    }
                }
            }
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
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

    // Progress Polling for UI and saving state
    LaunchedEffect(exoPlayer, isEnded) {
        while (!isEnded) {
            val d = exoPlayer.duration
            val p = exoPlayer.currentPosition
            
            duration = d.coerceAtLeast(0L)
            playbackPosition = p.coerceAtLeast(0L)
            
            if (d > 0) {
                // Save progress periodically (every 5s)
                if (p > 5000 && (d - p) > 10000) { // Don't save if just started or nearly finished
                     cacheManager.savePlaybackProgress(episodeUrl, p, d)
                }
            }
            delay(500) // Poll faster for smooth UI progress
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

            // Custom Player Controls Overlay
            PlayerControls(
                isPlaying = isPlaying,
                currentPosition = playbackPosition,
                duration = duration,
                formattedTime = "${formatTime(playbackPosition)} / ${formatTime(duration)}",
                onTogglePlayPause = {
                    if (exoPlayer.isPlaying) {
                        exoPlayer.pause()
                    } else {
                        exoPlayer.play()
                    }
                },
                onSeek = { newPosition ->
                    exoPlayer.seekTo(newPosition)
                    playbackPosition = newPosition
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerControls(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    formattedTime: String,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
    var isSeekBarFocused by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.8f)
                    )
                )
            )
            .padding(horizontal = 48.dp, vertical = 32.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Current time / Duration
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp
                ),
                color = CinematicText
            )

            // 2. Seek bar (Interacting with D-pad)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .onFocusChanged { isSeekBarFocused = it.isFocused }
                    .focusable()
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            when (keyEvent.key) {
                                Key.DirectionLeft -> {
                                    val newPos = (currentPosition - 10000).coerceAtLeast(0L)
                                    onSeek(newPos)
                                    true
                                }
                                Key.DirectionRight -> {
                                    val newPos = (currentPosition + 10000).coerceAtMost(duration)
                                    onSeek(newPos)
                                    true
                                }
                                else -> false
                            }
                        } else false
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                // Track
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isSeekBarFocused) 10.dp else 8.dp),
                    color = CinematicAccent,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                
                // Thumb
                if (isSeekBarFocused) {
                    val thumbOffset = maxWidth * progress
                    Box(
                        modifier = Modifier
                            .offset(x = thumbOffset - 8.dp)
                            .size(16.dp)
                            .background(Color.White, CircleShape)
                    )
                }
            }

            // 3. Play/Pause button and Cog
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onTogglePlayPause) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Cog button
                IconButton(onClick = { /* Design only */ }) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}