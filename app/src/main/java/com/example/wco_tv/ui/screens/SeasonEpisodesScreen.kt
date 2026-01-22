package com.example.wco_tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.*
import com.example.wco_tv.CinematicAccent
import com.example.wco_tv.CinematicBackground
import com.example.wco_tv.CinematicSurface
import com.example.wco_tv.CinematicText
import com.example.wco_tv.CinematicTextSecondary
import com.example.wco_tv.data.model.CartoonDetails
import com.example.wco_tv.data.model.Episode
import com.example.wco_tv.data.local.CacheManager
import kotlinx.coroutines.delay

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SeasonEpisodesScreen(
    seasonId: String,
    cartoonDetails: CartoonDetails?,
    cacheManager: CacheManager,
    onEpisodeClick: (Episode) -> Unit,
    onBack: () -> Unit
) {
    if (cartoonDetails == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Error: No Details Loaded", color = Color.Red)
            Button(onClick = onBack) { Text("Back") }
        }
        return
    }

    val seasonName = cartoonDetails.seasonNames[seasonId] ?: "Season ${seasonId.replace("s", "")}"
    // Assuming source is descending (Latest..First), reverse to get Ascending (Ep 1..Latest)
    val episodes = remember(cartoonDetails, seasonId) {
        cartoonDetails.episodes.filter { it.seasonId == seasonId }.reversed()
    }
    
    val focusRequester = remember { FocusRequester() }

    // Load progress
    var progressMap by remember { mutableStateOf<Map<String, CacheManager.PlaybackInfo>>(emptyMap()) }
    LaunchedEffect(Unit) {
        progressMap = cacheManager.getAllPlaybackProgress()
    }

    // Auto-focus first episode when list loads
    LaunchedEffect(episodes.isNotEmpty()) {
        if (episodes.isNotEmpty()) {
            delay(100)
            focusRequester.requestFocus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CinematicBackground)
            .padding(48.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = cartoonDetails.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = CinematicTextSecondary
                )
                Text(
                    text = seasonName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = CinematicText
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // Episode List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(episodes) { index, episode ->
                EpisodeItem(
                    episode = episode,
                    index = index + 1,
                    progress = progressMap[episode.url],
                    modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
                    onClick = { onEpisodeClick(episode) }
                )
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun EpisodeItem(
    episode: Episode,
    index: Int,
    progress: CacheManager.PlaybackInfo?,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(0.6f)
            .onFocusChanged { isFocused = it.isFocused },
        colors = ButtonDefaults.colors(
            containerColor = CinematicSurface,
            focusedContainerColor = CinematicAccent
        ),
        shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${index.toString().padStart(2, '0')}. ${episode.title}",
                    color = CinematicText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                // We can keep the "Eps" style count logic or just show playback hint
                if (progress != null && progress.position > 0) {
                    Text(
                        text = "Resume",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isFocused) Color.White else CinematicAccent,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            // Progress Bar (Integrated into the bottom of the button)
            if (progress != null && progress.duration > 0 && progress.position > 0) {
                val fraction = (progress.position.toFloat() / progress.duration.toFloat()).coerceIn(0f, 1f)
                if (fraction in 0.05f..0.95f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction)
                                .fillMaxHeight()
                                .background(if (isFocused) Color.White else CinematicAccent)
                        )
                    }
                }
            }
        }
    }
}