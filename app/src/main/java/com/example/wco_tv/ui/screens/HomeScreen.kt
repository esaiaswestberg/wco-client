package com.example.wco_tv.ui.screens

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.foundation.lazy.grid.rememberTvLazyGridState
import androidx.tv.material3.*
import androidx.compose.material3.CircularProgressIndicator
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.wco_tv.CinematicAccent
import com.example.wco_tv.CinematicSurface
import com.example.wco_tv.CinematicText
import com.example.wco_tv.CinematicTextSecondary
import com.example.wco_tv.data.model.Cartoon
import com.example.wco_tv.data.remote.TvMazeRepository
import kotlinx.coroutines.delay
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    cartoons: List<Cartoon>,
    favorites: Set<String> = emptySet(),
    isLoading: Boolean,
    errorMessage: String?,
    onRetry: () -> Unit,
    onCartoonClick: (Cartoon) -> Unit,
    onSettingsClick: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val gridState = rememberTvLazyGridState()
    val focusRequester = remember { FocusRequester() }
    
    // Filter logic
    val filteredCartoons = remember(searchQuery, cartoons) {
        if (searchQuery.isBlank()) cartoons
        else cartoons.filter { it.title.contains(other = searchQuery, ignoreCase = true) }
    }
    
    // Auto-focus first item when list loads
    LaunchedEffect(filteredCartoons.isNotEmpty()) {
        if (filteredCartoons.isNotEmpty()) {
            delay(100) // Small delay to let UI compose
            focusRequester.requestFocus()
        }
    }

    // Dynamic Background State
    var activeBackground by remember { mutableStateOf<String?>(null) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        // Dynamic Background Layer with smooth transition
        Crossfade(
            targetState = activeBackground,
            animationSpec = tween(durationMillis = 800),
            modifier = Modifier.fillMaxSize(),
            label = "BackgroundCrossfade"
        ) { url ->
            if (url != null) {
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(0.3f) // Dimmed for readability
                )
            } else {
                 // Empty box or default background color if no image is selected
                 Box(modifier = Modifier.fillMaxSize().background(Color.Transparent))
            }
        }
        
        // Gradient Overlay (Darken edges and top/bottom) - remains static on top
        if (activeBackground != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.8f), // Top darkening
                                Color.Black.copy(alpha = 0.5f), // Middle clear-ish
                                Color.Black.copy(alpha = 0.9f)  // Bottom darkening
                            )
                        )
                    )
            )
        }

        if (cartoons.isEmpty() && !isLoading) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (errorMessage != null) "Connection Error" else "WCO TV",
                    style = MaterialTheme.typography.displayMedium,
                    color = CinematicText
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.colors(
                        containerColor = CinematicSurface,
                        focusedContainerColor = CinematicAccent
                    )
                ) {
                    Text(text = "Retry Scan", color = CinematicText)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onSettingsClick,
                    colors = ButtonDefaults.colors(
                        containerColor = CinematicSurface,
                        focusedContainerColor = CinematicAccent
                    )
                ) {
                    Text(text = "Settings", color = CinematicText)
                }
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(text = errorMessage, color = Color.Red.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                }
            }
        } else if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                 Column(horizontalAlignment = Alignment.CenterHorizontally) {
                     CircularProgressIndicator(color = CinematicAccent)
                     Spacer(modifier = Modifier.height(16.dp))
                     Text(text = "Scanning Library...", color = CinematicTextSecondary)
                 }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 24.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "WCO FLIX",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                            color = CinematicAccent
                        )
                        Text(
                            text = "${filteredCartoons.size} Titles Available",
                            style = MaterialTheme.typography.labelMedium,
                            color = CinematicTextSecondary
                        )
                    }
                    
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Search Input (Custom styled)
                        Box(
                            modifier = Modifier
                                .width(350.dp)
                                .background(CinematicSurface, RoundedCornerShape(50))
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = CinematicText,
                                    fontSize = 16.sp
                                ),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text("Search cartoons...", color = CinematicTextSecondary.copy(alpha = 0.5f))
                                    }
                                    innerTextField()
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Button(
                            onClick = onSettingsClick,
                            shape = ButtonDefaults.shape(RoundedCornerShape(50)),
                            colors = ButtonDefaults.colors(
                                containerColor = CinematicSurface,
                                focusedContainerColor = CinematicAccent
                            )
                        ) {
                            Text("Settings", color = CinematicText)
                        }
                    }
                }

                TvLazyVerticalGrid(
                    state = gridState,
                    columns = TvGridCells.Fixed(5),
                    // Optimized top padding to balance space and prevent clipping
                    contentPadding = PaddingValues(top = 32.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredCartoons.size) { index ->
                        val cartoon = filteredCartoons[index]
                        CartoonCard(
                            cartoon = cartoon, 
                            isFavorite = favorites.contains(cartoon.title),
                            modifier = if (index == 0) Modifier.focusRequester(focusRequester) else Modifier,
                            onClick = { onCartoonClick(cartoon) },
                            onFocus = {
                                // Update background when focused
                                val cached = TvMazeRepository.getCachedArtwork(cartoon.title)
                                if (cached != null) {
                                    activeBackground = cached.backgroundUrl ?: cached.posterUrl
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CartoonCard(
    cartoon: Cartoon,
    isFavorite: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onFocus: () -> Unit = {}
) {
    var isFocused by remember { mutableStateOf(false) }
    var artworkUrl by remember { mutableStateOf<String?>(null) }
    
    // Notify parent on focus
    LaunchedEffect(isFocused) {
        if (isFocused) {
            onFocus()
        }
    }

    // Lazy load artwork with debounce
    LaunchedEffect(cartoon.title) {
        // Debounce: Wait 500ms to ensure user is actually looking at this item
        // and not just flinging past it. This saves bandwidth and API limits.
        delay(500)
        val artwork = TvMazeRepository.searchShow(cartoon.title)
        if (artwork?.posterUrl != null) {
            artworkUrl = artwork.posterUrl
            // Also notify focus if we just loaded the art and are currently focused
            if (isFocused) onFocus()
        }
    }
    
    // Generate a deterministic color based on the title hash (Fallback)
    val hash = cartoon.title.hashCode()
    val hue = kotlin.math.abs(hash % 360).toFloat()
    val baseColor = Color.hsv(hue, 0.6f, 0.4f)
    val gradientBrush = Brush.verticalGradient(
        colors = listOf(
            baseColor,
            Color.Black.copy(alpha = 0.8f)
        )
    )

    val scale = if (isFocused) 1.1f else 1f

    Surface(
        onClick = onClick,
        modifier = modifier
            .aspectRatio(2f / 3f) // Standard poster aspect ratio
            .onFocusChanged { isFocused = it.isFocused }
            .zIndex(if (isFocused) 1f else 0f)
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            ),
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(8.dp)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = androidx.compose.foundation.BorderStroke(
                    width = if (isFocused) 2.dp else 0.dp,
                    color = if (isFocused) CinematicAccent else Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(CinematicSurface) // Default background while loading
        ) {
            if (artworkUrl != null) {
                // Show fetched artwork
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.9f)
                                )
                            )
                        )
                )
            } else {
                // Show Fallback Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(gradientBrush)
                )
            }

            // Favorite Star Indicator
            if (isFavorite) {
                Text(
                    text = "★",
                    color = Color(0xFFFFD700), // Gold
                    fontSize = 20.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .zIndex(2f)
                )
            }

            // Text Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                 Text(
                    text = cartoon.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        shadow = androidx.compose.ui.graphics.Shadow(
                            color = Color.Black,
                            blurRadius = 4f
                        )
                    ),
                    color = CinematicText,
                    textAlign = TextAlign.Start,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}