package com.example.wco_tv

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.wco_tv.data.local.CacheManager
import com.example.wco_tv.data.model.Cartoon
import com.example.wco_tv.data.model.CartoonDetails
import com.example.wco_tv.data.model.Episode
import com.example.wco_tv.ui.screens.DetailsScreen
import com.example.wco_tv.ui.screens.HomeScreen
import com.example.wco_tv.ui.screens.SeasonEpisodesScreen
import com.example.wco_tv.ui.screens.PlayerScreen
import com.example.wco_tv.data.remote.TvMazeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.net.URLDecoder

// WORKING HEADERS CONSTANTS
const val WORKING_USER_AGENT = "Mozilla/5.0 (X11; Linux x86_64; rv:146.0) Gecko/20100101 Firefox/146.0"

// CINEMATIC THEME COLORS
val CinematicBackground = Color(0xFF0F1115) // Deep Dark Blue-Grey
val CinematicSurface = Color(0xFF1C1E26)    // Slightly lighter for cards
val CinematicAccent = Color(0xFF3D5AFE)     // Vibrant Blue for focus
val CinematicText = Color(0xFFEEEEEE)       // Off-white text
val CinematicTextSecondary = Color(0xFFAAAAAA)

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    background = CinematicBackground,
                    surface = CinematicSurface,
                    primary = CinematicAccent,
                    onSurface = CinematicText
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF14161C), // Top (slightly lighter)
                                        Color(0xFF0B0D10)  // Bottom (darker)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                       AppNavigation()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val navController = rememberNavController()
    val cacheManager = remember { 
        CacheManager(context).also {
            TvMazeRepository.initialize(it)
        }
    }
    
    // Scraper State
    var urlToScrape by remember { mutableStateOf<String?>(null) }
    var onScrapeResult by remember { mutableStateOf<((String, String?) -> Unit)?>(null) }
    
    // Global State
    var cartoons by remember { mutableStateOf<List<Cartoon>>(emptyList()) }
    var favorites by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isHomeLoading by remember { mutableStateOf(true) }
    var homeError by remember { mutableStateOf<String?>(null) }
    var selectedDetails by remember { mutableStateOf<CartoonDetails?>(null) }
    
    // Player State
    var isPlayerLoading by remember { mutableStateOf(false) }
    var currentVideoUrl by remember { mutableStateOf<String?>(null) }
    var currentEpisodeIndex by remember { mutableStateOf(-1) }

    // Helper to request HTML
    val requestHtml: (String, (String, String?) -> Unit) -> Unit = { url, callback ->
        urlToScrape = url
        onScrapeResult = callback
    }
    
    // Reusable function to play an episode
    val playEpisode: (Episode) -> Unit = { episode ->
        isPlayerLoading = true
        val episodeUrl = if (episode.url.startsWith("http")) episode.url else "https://www.wcoflix.tv${episode.url}"
        Log.d("WCO_TV", "Resolving Episode URL: $episodeUrl")
        
        requestHtml(episodeUrl) { episodeHtml, _ ->
            val iframeUrl = parseIframeUrl(episodeHtml)
            if (iframeUrl != null) {
                Log.d("WCO_TV", "Found Iframe URL: $iframeUrl")
                requestHtml(iframeUrl) { iframeHtml, detectedVideoUrl ->
                    val videoUrl = detectedVideoUrl ?: parseVideoUrl(iframeHtml)
                    if (videoUrl != null) {
                        Log.d("WCO_TV", "Resolved Video URL: $videoUrl")
                        currentVideoUrl = videoUrl
                        isPlayerLoading = false
                        if (navController.currentDestination?.route != "player") {
                            navController.navigate("player")
                        }
                    } else {
                        Log.e("WCO_TV", "Could not find video URL")
                        isPlayerLoading = false
                    }
                }
            } else {
                Log.e("WCO_TV", "Could not find iframe URL")
                isPlayerLoading = false
            }
        }
    }

    val toggleFavorite: (String) -> Unit = { title ->
        val newFavorites = favorites.toMutableSet()
        if (newFavorites.contains(title)) {
            newFavorites.remove(title)
        } else {
            newFavorites.add(title)
        }
        favorites = newFavorites
        cacheManager.saveFavorites(newFavorites)
    }

    // Initial Load
    LaunchedEffect(Unit) {
        // Load Favorites
        val cachedFavorites = cacheManager.getFavorites()
        if (cachedFavorites.isNotEmpty()) {
            favorites = cachedFavorites
        }

        if (cartoons.isEmpty()) {
            val cached = cacheManager.getCartoons()
            if (cached != null && cached.isNotEmpty()) {
                Log.d("WCO_TV", "Loaded ${cached.size} cartoons from cache")
                cartoons = cached
                isHomeLoading = false
            } else {
                requestHtml("https://www.wcoflix.tv/cartoon-list") { html, _ ->
                    val list = parseCartoons(html)
                    if (list.isNotEmpty()) {
                        cartoons = list
                        cacheManager.saveCartoons(list)
                        isHomeLoading = false
                    } else {
                        homeError = "Failed to load cartoon list."
                        isHomeLoading = false
                    }
                }
            }
        }
    }

    // Background Scraper
    if (urlToScrape != null) {
        Box(modifier = Modifier.size(0.dp)) {
            ScraperWebView(
                url = urlToScrape!!,
                onResult = { html, videoUrl ->
                    scope.launch(Dispatchers.Main) {
                        val currentUrl = urlToScrape
                        val callback = onScrapeResult
                        callback?.invoke(html, videoUrl)
                        if (urlToScrape == currentUrl) {
                            urlToScrape = null
                            onScrapeResult = null
                        }
                    }
                },
                onError = { err ->
                    scope.launch(Dispatchers.Main) {
                        Log.e("WCO_TV", "Scraper error: $err")
                        urlToScrape = null
                        onScrapeResult = null
                        isPlayerLoading = false
                    }
                }
            )
        }
    }
    
    // Full Screen Loading Overlay
    if (isPlayerLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.85f))
                .zIndex(100f), // Ensure it covers everything
            contentAlignment = Alignment.Center
        ) {
            Text("Resolving Video URL...", color = Color.Yellow)
        }
    }

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            // Sort: Favorites first, then alphabetical
            val sortedCartoons = remember(cartoons, favorites) {
                cartoons.sortedWith(compareByDescending<Cartoon> { it.title in favorites }.thenBy { it.title })
            }

            HomeScreen(
                cartoons = sortedCartoons,
                favorites = favorites,
                isLoading = isHomeLoading,
                errorMessage = homeError,
                onRetry = {
                    isHomeLoading = true
                    homeError = null
                    requestHtml("https://www.wcoflix.tv/cartoon-list") { html, _ ->
                        val list = parseCartoons(html)
                        if (list.isNotEmpty()) {
                            cartoons = list
                            cacheManager.saveCartoons(list)
                            isHomeLoading = false
                        } else {
                            homeError = "Failed to load cartoon list."
                            isHomeLoading = false
                        }
                    }
                },
                onCartoonClick = { cartoon ->
                    selectedDetails = null
                    val encodedUrl = URLEncoder.encode(cartoon.link, "UTF-8")
                    navController.navigate("details/$encodedUrl")
                }
            )
        }
        composable(
            "details/{url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val url = backStackEntry.arguments?.getString("url") ?: ""
            DetailsScreen(
                url = url,
                isFavorite = selectedDetails?.title?.let { it in favorites } ?: false,
                onToggleFavorite = { selectedDetails?.title?.let { toggleFavorite(it) } },
                requestHtml = { u, cb -> requestHtml(u) { h, _ -> cb(h) } },
                onBack = { navController.popBackStack() },
                onDetailsLoaded = { details ->
                    selectedDetails = details
                },
                onSeasonClick = { seasonId ->
                    navController.navigate("episodes/$seasonId")
                }
            )
        }
        composable(
            "episodes/{seasonId}",
            arguments = listOf(navArgument("seasonId") { type = NavType.StringType })
        ) { backStackEntry ->
            val seasonId = backStackEntry.arguments?.getString("seasonId") ?: "s1"
            
            SeasonEpisodesScreen(
                seasonId = seasonId,
                cartoonDetails = selectedDetails,
                cacheManager = cacheManager,
                onBack = { navController.popBackStack() },
                onEpisodeClick = { episode ->
                    // Find index of clicked episode
                    val index = selectedDetails?.episodes?.indexOf(episode) ?: -1
                    if (index != -1) {
                        currentEpisodeIndex = index
                        playEpisode(episode)
                    }
                }
            )
        }
        composable("player") {
            currentVideoUrl?.let { url ->
                // Logic to find next episode
                val episodes = selectedDetails?.episodes
                val nextEpisode = if (episodes != null && currentEpisodeIndex >= 0 && currentEpisodeIndex + 1 < episodes.size) {
                    episodes[currentEpisodeIndex + 1]
                } else null
                
                val currentEpisode = if (episodes != null && currentEpisodeIndex >= 0) episodes[currentEpisodeIndex] else null

                if (currentEpisode != null) {
                    PlayerScreen(
                        videoUrl = url,
                        episodeUrl = currentEpisode.url, // Pass stable WCO URL for cache key
                        cacheManager = cacheManager,
                        nextEpisodeTitle = nextEpisode?.title,
                        onPlayNext = {
                            if (nextEpisode != null) {
                                currentEpisodeIndex++
                                playEpisode(nextEpisode)
                            }
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

// Parsing helpers for video
fun parseIframeUrl(html: String): String? {
    val doc = org.jsoup.Jsoup.parse(html)
    return doc.select("div.iframe-16x9 iframe").attr("src").ifEmpty {
        doc.select("iframe#cizgi-js-0").attr("src")
    }.let { 
        if (it.isNotEmpty() && !it.startsWith("http")) "https:$it" else it.ifEmpty { null }
    }
}

fun parseVideoUrl(html: String): String? {
    val doc = org.jsoup.Jsoup.parse(html)
    val videoTag = doc.select("video source").attr("src").ifEmpty {
        doc.select("video").attr("src")
    }
    if (videoTag.isNotEmpty()) return videoTag

    val scripts = doc.select("script").map { it.data() }
    for (script in scripts) {
        val regex = """["'](https?://[^"']+\.(?:mp4|m3u8|flv)[^"']*)["']""".toRegex()
        val match = regex.find(script)
        if (match != null) return match.groupValues[1]
        
        val fileRegex = """file\s*:\s*["']([^"']+)["']""".toRegex()
        val fileMatch = fileRegex.find(script)
        if (fileMatch != null) return fileMatch.groupValues[1]
    }
    return null
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ScraperWebView(url: String, onResult: (String, String?) -> Unit, onError: (String) -> Unit) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.userAgentString = WORKING_USER_AGENT
                
                addJavascriptInterface(object {
                    @JavascriptInterface
                    fun processHTML(html: String, videoUrl: String?) {
                        onResult(html, if (videoUrl.isNullOrEmpty()) null else videoUrl)
                    }
                    @JavascriptInterface
                    fun log(msg: String) {
                        Log.d("WCO_TV_JS", msg)
                    }
                }, "HTMLOUT")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        Log.d("WCO_TV", "Page finished loading: $url")
                        
                        val jsPoller = """
                            (function() {
                                window.HTMLOUT.log("Poller started for " + window.location.href);
                                var attempts = 0;
                                var maxAttempts = 100; // 30 seconds
                                var interval = setInterval(function() {
                                    var list = document.querySelector('div.ddmcc');
                                    var details = document.querySelector('div#episodeList') || document.querySelector('div#sidebar_cat');
                                    var iframe = document.querySelector('iframe');
                                    var video = document.querySelector('video');
                                    var videoSrc = (video && video.src && video.src.indexOf('http') === 0) ? video.src : "";
                                    
                                    if (list || details || videoSrc || (iframe && attempts > 15)) {
                                        window.HTMLOUT.log("Found content at attempt " + attempts + (videoSrc ? " (Video source detected)" : ""));
                                        clearInterval(interval);
                                        window.HTMLOUT.processHTML(document.documentElement.outerHTML, videoSrc);
                                    }
                                    
                                    attempts++;
                                    if (attempts >= maxAttempts) {
                                        window.HTMLOUT.log("Timeout reaching max attempts");
                                        clearInterval(interval);
                                        window.HTMLOUT.processHTML(document.documentElement.outerHTML, null);
                                    }
                                }, 300);
                            })();
                        """.trimIndent()
                        view?.evaluateJavascript(jsPoller, null)
                    }
                    override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                        Log.e("WCO_TV", "WebView Error: $description")
                        onError(description ?: "Unknown error")
                    }
                }
            }
        },
        update = { webView ->
            if (webView.url != url) {
                Log.d("WCO_TV", "WebView Update: Loading $url")
                val headers = mapOf("Referer" to "https://www.wcoflix.tv/")
                // Clear cache to prevent stale player data or 403s
                webView.clearCache(true)
                webView.loadUrl(url, headers)
            }
        }
    )
}

fun parseCartoons(html: String): List<Cartoon> {
    val doc = org.jsoup.Jsoup.parse(html)
    val cartoonList = mutableListOf<Cartoon>()
    val links = doc.select("div.ddmcc ul li a")
    for (link in links) {
        val title = link.text()
        val href = link.attr("href")
        if (title.isNotBlank() && href.isNotBlank()) {
            cartoonList.add(Cartoon(title, href))
        }
    }
    return cartoonList
}