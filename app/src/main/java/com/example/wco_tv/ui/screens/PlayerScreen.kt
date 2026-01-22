package com.example.wco_tv.ui.screens

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import androidx.tv.material3.*
import com.example.wco_tv.CinematicAccent
import com.example.wco_tv.CinematicSurface
import com.example.wco_tv.CinematicText
import com.example.wco_tv.WORKING_USER_AGENT
import com.example.wco_tv.data.local.CacheManager
import com.example.wco_tv.data.model.VideoQuality
import kotlinx.coroutines.delay

enum class SettingsTab {
    Main, Quality, Speed
}

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

    // Settings State
    var isSettingsOpen by remember { mutableStateOf(false) }
    var activeSettingsTab by remember { mutableStateOf(SettingsTab.Main) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

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

        val dataSourceFactory = DefaultHttpDataSource.Factory ()
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
        // Re-apply speed on quality change
        exoPlayer.playbackParameters = PlaybackParameters(playbackSpeed)
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
            androidx.tv.material3.Text(
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
                onSettingsClick = {
                    isSettingsOpen = true
                    activeSettingsTab = SettingsTab.Main
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .focusProperties { canFocus = !isSettingsOpen }
            )

            // Settings Menu Side Panel
            SettingsMenu(
                isOpen = isSettingsOpen,
                activeTab = activeSettingsTab,
                onClose = { isSettingsOpen = false },
                onTabChange = { activeSettingsTab = it },
                qualities = qualities,
                currentUrl = currentUrl,
                onQualitySelect = { currentUrl = it },
                currentSpeed = playbackSpeed,
                onSpeedSelect = {
                    playbackSpeed = it
                    exoPlayer.playbackParameters = PlaybackParameters(it)
                }
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
    onSettingsClick: () -> Unit,
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
            androidx.tv.material3.Text(
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
                                Key.Enter, Key.DirectionCenter, Key.NumPadEnter -> {
                                    onTogglePlayPause()
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
                IconButton(onClick = onSettingsClick) {
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

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsMenu(
    isOpen: Boolean,
    activeTab: SettingsTab,
    onClose: () -> Unit,
    onTabChange: (SettingsTab) -> Unit,
    qualities: List<VideoQuality>,
    currentUrl: String,
    onQualitySelect: (String) -> Unit,
    currentSpeed: Float,
    onSpeedSelect: (Float) -> Unit
) {
    val firstItemFocusRequester = remember { FocusRequester() }

    LaunchedEffect(isOpen, activeTab) {
        if (isOpen) {
            delay(250) // Ensure panel is visible and composed
            firstItemFocusRequester.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        // Scrim background to capture focus
        if (isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable { onClose() }
            )
        }

        AnimatedVisibility(
            visible = isOpen,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .fillMaxHeight()
                .width(350.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(CinematicSurface)
                    .onKeyEvent {
                        if (it.key == Key.Back && it.type == KeyEventType.KeyDown) {
                            if (activeTab == SettingsTab.Main) {
                                onClose()
                            } else {
                                onTabChange(SettingsTab.Main)
                            }
                            true
                        } else false
                    }
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (activeTab != SettingsTab.Main) {
                            androidx.tv.material3.Surface(
                                onClick = { onTabChange(SettingsTab.Main) },
                                modifier = Modifier.focusRequester(firstItemFocusRequester),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = Color.Transparent,
                                    focusedContainerColor = Color.White.copy(alpha = 0.15f)
                                ),
                                shape = ClickableSurfaceDefaults.shape(CircleShape)
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack, 
                                    contentDescription = "Back", 
                                    tint = CinematicText, 
                                    modifier = Modifier.padding(8.dp)
                                )
                            }
                        }
                        androidx.tv.material3.Text(
                            text = when (activeTab) {
                                SettingsTab.Main -> "Settings"
                                SettingsTab.Quality -> "Video Quality"
                                SettingsTab.Speed -> "Playback Speed"
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            color = CinematicText,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        when (activeTab) {
                            SettingsTab.Main -> {
                                item {
                                    SettingsItem(
                                        label = "Quality",
                                        value = qualities.find { it.url == currentUrl }?.label ?: "Auto",
                                        icon = Icons.Default.HighQuality,
                                        onClick = { onTabChange(SettingsTab.Quality) },
                                        modifier = Modifier.focusRequester(firstItemFocusRequester)
                                    )
                                }
                                item {
                                    SettingsItem(
                                        label = "Speed",
                                        value = "${currentSpeed}x",
                                        icon = Icons.Default.Speed,
                                        onClick = { onTabChange(SettingsTab.Speed) }
                                    )
                                }
                            }
                            SettingsTab.Quality -> {
                                itemsIndexed(qualities) { index, quality ->
                                    SettingsOption(
                                        label = quality.label,
                                        isSelected = quality.url == currentUrl,
                                        onClick = {
                                            onQualitySelect(quality.url)
                                            onClose()
                                        },
                                        modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                                    )
                                }
                            }
                            SettingsTab.Speed -> {
                                val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                                itemsIndexed(speeds) { index, speed ->
                                    SettingsOption(
                                        label = "${speed}x",
                                        isSelected = speed == currentSpeed,
                                        onClick = {
                                            onSpeedSelect(speed)
                                            onClose()
                                        },
                                        modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { isFocused = it.isFocused },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, CinematicAccent),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon, 
                contentDescription = null, 
                tint = if (isFocused) Color.White else CinematicText, 
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.padding(start = 16.dp).weight(1f)) {
                androidx.tv.material3.Text(label, color = CinematicText, style = MaterialTheme.typography.bodyLarge)
                androidx.tv.material3.Text(value, color = CinematicAccent, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                Icons.Default.ChevronRight, 
                contentDescription = null, 
                tint = if (isFocused) Color.White else CinematicText.copy(alpha = 0.5f)
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsOption(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .onFocusChanged { isFocused = it.isFocused },
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.White.copy(alpha = 0.15f)
        ),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, CinematicAccent),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            androidx.tv.material3.Text(
                label, 
                color = if (isFocused) Color.White else CinematicText, 
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (isSelected) {
                Icon(
                    Icons.Default.Check, 
                    contentDescription = "Selected", 
                    tint = CinematicAccent
                )
            }
        }
    }
}
