package com.example.wco_tv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.wco_tv.CinematicAccent
import com.example.wco_tv.CinematicSurface
import com.example.wco_tv.CinematicText
import com.example.wco_tv.data.remote.WcoStatusFetcher
import kotlinx.coroutines.launch

// Alias for standard Material3 components to avoid naming conflicts
import androidx.compose.material3.Button as MobileButton
import androidx.compose.material3.ButtonDefaults as MobileButtonDefaults
import androidx.compose.material3.Text as MobileText
import androidx.compose.material3.MaterialTheme as MobileMaterialTheme

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentUrl: String,
    onUrlSave: (String) -> Unit,
    onBack: () -> Unit
) {
    var urlText by remember { mutableStateOf(currentUrl) }
    var availableDomains by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoadingDomains by remember { mutableStateOf(false) }
    var domainError by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    
    val configuration = LocalConfiguration.current
    val isCompact = configuration.screenWidthDp < 600

    // Fetch domains helper
    fun fetchDomains() {
        scope.launch {
            isLoadingDomains = true
            domainError = null
            val domains = WcoStatusFetcher.fetchAvailableDomains()
            if (domains.isNotEmpty()) {
                availableDomains = domains
            } else {
                domainError = "Could not fetch mirror list."
            }
            isLoadingDomains = false
        }
    }

    // Auto-fetch on entry if list is empty
    LaunchedEffect(Unit) {
        fetchDomains()
    }
    
    if (isCompact) {
        // Mobile Layout (Single Column) - Using Standard Material3 Components
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
             MobileText(
                text = "Settings",
                style = MobileMaterialTheme.typography.displaySmall,
                color = CinematicText
            )

            // URL Entry Section
            Column(modifier = Modifier.fillMaxWidth()) {
                MobileText(
                    text = "Base Domain URL",
                    style = MobileMaterialTheme.typography.titleMedium,
                    color = CinematicText
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                MobileText(
                    text = "Current: $currentUrl",
                    style = MobileMaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CinematicSurface, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    BasicTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onUrlSave(urlText)
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (urlText.isEmpty()) {
                                MobileText("Enter URL...", color = Color.Gray)
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MobileButton(
                        onClick = { onUrlSave(urlText) },
                        modifier = Modifier.weight(1f),
                        colors = MobileButtonDefaults.buttonColors(
                            containerColor = CinematicSurface
                        )
                    ) {
                        MobileText("Save", color = CinematicText)
                    }
                    
                    MobileButton(
                        onClick = { 
                            urlText = "https://www.wcoflix.tv"
                            onUrlSave(urlText)
                        },
                        modifier = Modifier.weight(1f),
                        colors = MobileButtonDefaults.buttonColors(
                            containerColor = CinematicSurface
                        )
                    ) {
                        MobileText("Reset", color = CinematicText)
                    }
                }
            }
            
            // Mirrors Section
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MobileText(
                        text = "Mirrors",
                        style = MobileMaterialTheme.typography.titleMedium,
                        color = CinematicText
                    )
                    
                    androidx.compose.material3.IconButton(
                        onClick = { fetchDomains() }
                    ) {
                       Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CinematicText) 
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                
                if (isLoadingDomains) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CinematicAccent)
                    }
                } else if (domainError != null) {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        MobileText(domainError!!, color = Color.Red)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableDomains) { domain ->
                            val isSelected = urlText == domain
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) CinematicAccent.copy(alpha = 0.2f) else CinematicSurface)
                                    .clickable { urlText = domain }
                                    .padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MobileText(
                                        text = domain, 
                                        style = MobileMaterialTheme.typography.bodyLarge,
                                        color = CinematicText
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check, 
                                            contentDescription = "Selected",
                                            tint = Color(0xFF4CAF50) // Green check
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            MobileButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth(),
                colors = MobileButtonDefaults.buttonColors(
                    containerColor = CinematicSurface
                )
            ) {
                MobileText("Back", color = CinematicText)
            }
        }
    } else {
        // TV Layout (Two Columns)
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp)
        ) {
            // Left Column: Manual Entry & Actions
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Base Domain URL",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Current: $currentUrl",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CinematicSurface, RoundedCornerShape(8.dp))
                        .padding(16.dp)
                ) {
                    BasicTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onUrlSave(urlText)
                            }
                        ),
                        decorationBox = { innerTextField ->
                            if (urlText.isEmpty()) {
                                Text("Enter URL...", color = Color.Gray)
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = { onUrlSave(urlText) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.colors(
                        containerColor = CinematicSurface,
                        focusedContainerColor = CinematicAccent
                    )
                ) {
                    Text("Save & Reload", color = CinematicText)
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = { 
                        urlText = "https://www.wcoflix.tv"
                        onUrlSave(urlText)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.colors(
                        containerColor = CinematicSurface,
                        focusedContainerColor = CinematicAccent
                    )
                ) {
                    Text("Reset to Default", color = CinematicText)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = onBack,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.colors(
                        containerColor = CinematicSurface,
                        focusedContainerColor = CinematicAccent
                    )
                ) {
                    Text("Back", color = CinematicText)
                }
            }

            // Right Column: Available Mirrors
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Available Mirrors",
                        style = MaterialTheme.typography.headlineSmall,
                        color = CinematicText
                    )
                    
                    Button(
                        onClick = { fetchDomains() },
                        scale = ButtonDefaults.scale(scale = 0.8f),
                        colors = ButtonDefaults.colors(
                            containerColor = CinematicSurface,
                            focusedContainerColor = CinematicAccent
                        )
                    ) {
                       Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = CinematicText) 
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                if (isLoadingDomains) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CinematicAccent)
                    }
                } else if (domainError != null) {
                     Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(domainError!!, color = Color.Red)
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableDomains) { domain ->
                            val isSelected = urlText == domain
                            Surface(
                                onClick = { urlText = domain },
                                shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(8.dp)),
                                colors = ClickableSurfaceDefaults.colors(
                                    containerColor = if (isSelected) CinematicAccent.copy(alpha = 0.2f) else CinematicSurface,
                                    focusedContainerColor = CinematicAccent,
                                    contentColor = CinematicText,
                                    focusedContentColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = domain, 
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check, 
                                            contentDescription = "Selected",
                                            tint = Color(0xFF4CAF50) // Green check
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
}
