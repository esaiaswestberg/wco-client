package com.example.wco_tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.*
import androidx.compose.material3.CircularProgressIndicator
import coil.compose.AsyncImage
import com.example.wco_tv.CinematicAccent
import com.example.wco_tv.CinematicSurface
import com.example.wco_tv.CinematicText
import com.example.wco_tv.CinematicTextSecondary
import com.example.wco_tv.data.model.CartoonDetails
import com.example.wco_tv.data.model.Episode
import org.jsoup.Jsoup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.wco_tv.data.remote.TvMazeRepository

import com.example.wco_tv.data.local.CacheManager

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DetailsScreen(
    url: String,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    requestHtml: (String, (String) -> Unit) -> Unit,
    onBack: () -> Unit,
    onSeasonClick: (String) -> Unit,
    onDetailsLoaded: (CartoonDetails) -> Unit
) {
    var details by remember { mutableStateOf<CartoonDetails?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var artwork by remember { mutableStateOf<CacheManager.Artwork?>(null) }
    val focusRequester = remember { FocusRequester() }
    
    // We need a scope for async operations initiated by callbacks
    val scope = rememberCoroutineScope()

    LaunchedEffect(url) {
        val fullUrl = if (url.startsWith("http")) url else "https://www.wcoflix.tv$url"
        requestHtml(fullUrl) { html ->
            val parsed = parseDetails(html)
            details = parsed
            isLoading = false
            onDetailsLoaded(parsed)
            
            // Background fetch for high-res artwork using the proper scope
            scope.launch(Dispatchers.IO) {
                val result = TvMazeRepository.searchShow(parsed.title)
                if (result != null) {
                    artwork = result
                }
            }
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = CinematicAccent)
        }
    } else {
        details?.let { data ->
            // Smart Image Selection
            val displayPoster = artwork?.posterUrl ?: data.imageUrl
            // Use specific background if available, otherwise fallback to poster
            val displayBackground = artwork?.backgroundUrl ?: displayPoster
            
            val availableSeasons = data.episodes.map { it.seasonId }.distinct().sortedBy { 
                it.replace("s", "").toIntOrNull() ?: 999 
            }

            // Auto-focus first season when list loads
            LaunchedEffect(availableSeasons.isNotEmpty()) {
                if (availableSeasons.isNotEmpty()) {
                    delay(100)
                    focusRequester.requestFocus()
                }
            }
            
            Box(modifier = Modifier.fillMaxSize()) {
                // Background Image (Dimmed)
                AsyncImage(
                    model = displayBackground,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.3f), // Slightly more visible since it might be a nice landscape
                    contentScale = ContentScale.Crop
                )
                
                // Gradient Overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.9f),
                                    Color.Black.copy(alpha = 0.6f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Content
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(48.dp)
                ) {
                    // Left: Poster
                    Card(
                        onClick = {},
                        shape = CardDefaults.shape(RoundedCornerShape(12.dp)),
                        modifier = Modifier
                            .width(300.dp)
                            .aspectRatio(2f/3f),
                        colors = CardDefaults.colors(containerColor = Color.Transparent)
                    ) {
                        AsyncImage(
                            model = displayPoster,
                            contentDescription = "Poster",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.width(48.dp))

                    // Right: Info & Seasons
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = data.title,
                                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                color = CinematicText,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Favorite Button
                            Button(
                                onClick = onToggleFavorite,
                                colors = ButtonDefaults.colors(
                                    containerColor = if (isFavorite) Color(0xFF332F00) else CinematicSurface,
                                    focusedContainerColor = if (isFavorite) Color(0xFF665C00) else CinematicAccent
                                ),
                                shape = ButtonDefaults.shape(RoundedCornerShape(50))
                            ) {
                                Text(
                                    text = if (isFavorite) "★ Favorited" else "☆ Add to Favorites",
                                    color = if (isFavorite) Color(0xFFFFD700) else CinematicText
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Genres
                        Row {
                            data.genres.take(4).forEach { genre ->
                                Box(
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .background(CinematicSurface, RoundedCornerShape(4.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = genre,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = CinematicTextSecondary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = data.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = CinematicTextSecondary,
                            maxLines = 4,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = "Seasons",
                            style = MaterialTheme.typography.titleLarge,
                            color = CinematicText
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            items(availableSeasons.size) { index ->
                                val seasonId = availableSeasons[index]
                                val seasonName = data.seasonNames[seasonId] ?: "Season ${seasonId.replace("s", "")}"
                                val episodeCount = data.episodes.count { it.seasonId == seasonId }
                                
                                Button(
                                    onClick = { onSeasonClick(seasonId) },
                                    modifier = Modifier
                                        .fillMaxWidth(0.6f)
                                        .then(if (index == 0) Modifier.focusRequester(focusRequester) else Modifier),
                                    colors = ButtonDefaults.colors(
                                        containerColor = CinematicSurface,
                                        focusedContainerColor = CinematicAccent
                                    ),
                                    shape = ButtonDefaults.shape(RoundedCornerShape(8.dp))
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(seasonName, color = CinematicText)
                                        Text("$episodeCount Eps", color = CinematicTextSecondary)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun parseDetails(html: String): CartoonDetails {
    val doc = Jsoup.parse(html)
    val title = doc.select("span.dynamic-name").firstOrNull()?.text() ?: 
                doc.select("div.video-title h1").text().ifEmpty { "Unknown Title" }
    val description = doc.select("div#sidebar_cat p").firstOrNull()?.text() ?: ""
    val imageUrl = doc.select("div#sidebar_cat img.img5").attr("src").let {
        when {
            it.startsWith("//") -> "https:$it"
            it.startsWith("/") -> "https://www.wcoflix.tv$it"
            else -> it
        }
    }
    val genres = doc.select("div#sidebar_cat a.genre-buton").map { it.text() }
    val seasonNames = doc.select("select#seasonFilter option").associate {
        val value = it.attr("value")
        val name = it.text()
        value to name
    }.filterKeys { it != "all" }
    val episodes = doc.select("div#episodeList a.dark-episode-item").map {
        Episode(
            title = it.select("span").firstOrNull()?.text() ?: it.text(),
            url = it.attr("href"),
            seasonId = it.attr("data-season").ifEmpty { "s1" }
        )
    }

    return CartoonDetails(title, description, imageUrl, genres, episodes, seasonNames)
}