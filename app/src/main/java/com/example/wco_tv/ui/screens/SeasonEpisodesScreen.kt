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
    
    // Load progress
    var progressMap by remember { mutableStateOf<Map<String, CacheManager.PlaybackInfo>>(emptyMap()) }
    LaunchedEffect(Unit) {
        progressMap = cacheManager.getAllPlaybackProgress()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CinematicBackground)
            .padding(48.dp)
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Button(
                onClick = onBack,
                modifier = Modifier.padding(end = 24.dp),
                colors = ButtonDefaults.colors(
                    containerColor = CinematicSurface,
                    focusedContainerColor = CinematicAccent
                ),
                shape = ButtonDefaults.shape(RoundedCornerShape(50))
            ) {
                Text(text = "← Back", modifier = Modifier.padding(horizontal = 8.dp))
            }
            
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
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { isFocused = it.isFocused },
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(12.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (isFocused) CinematicAccent else CinematicSurface,
            contentColor = CinematicText,
            focusedContainerColor = CinematicAccent,
            focusedContentColor = CinematicText
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            )
        )
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Episode Number
                Text(
                    text = index.toString().padStart(2, '0'),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isFocused) Color.White else CinematicTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.width(60.dp)
                )

                // Title
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                
                // Play Icon (Visual hint)
                if (isFocused) {
                    Text(text = "▶", color = Color.White)
                }
            }
            
            // Progress Bar
            if (progress != null && progress.duration > 0 && progress.position > 0) {
                val fraction = (progress.position.toFloat() / progress.duration.toFloat()).coerceIn(0f, 1f)
                // Don't show if basically finished (e.g. > 95%) or barely started (< 5%)
                if (fraction in 0.05f..0.95f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(Color.Black.copy(alpha = 0.5f))
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