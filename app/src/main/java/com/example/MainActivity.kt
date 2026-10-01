package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.components.ApiKeyDialog
import com.example.ui.components.AudioPlayerBottomBar
import com.example.ui.components.GlowingBackground
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.SpeechStudioScreen
import com.example.ui.screens.VoiceCloneWizardScreen
import com.example.ui.screens.VoiceLibraryScreen
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletBright
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanBright
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val showApiKeyDialog by viewModel.showApiKeyDialog.collectAsState()
    val selectedVoice by viewModel.selectedVoice.collectAsState()

    GlowingBackground {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            containerColor = Color.Transparent,
            bottomBar = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Floating Audio Player bar
                        AudioPlayerBottomBar(
                            playerHelper = viewModel.playerHelper,
                            title = selectedVoice.name,
                            subtitle = "Cartesia Voice Player"
                        )

                        // Bottom Navigation Bar
                        NavigationBar(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 1.dp,
                                    color = StudioBorder.copy(alpha = 0.5f)
                                ),
                            containerColor = StudioSurface.copy(alpha = 0.95f),
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            NavigationBarItem(
                                selected = currentScreen == AppScreen.HOME,
                                onClick = { viewModel.navigateTo(AppScreen.HOME) },
                                icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                                label = { Text("হোম", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = NeonCyanBright,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.VOICE_CLONE,
                                onClick = { viewModel.navigateTo(AppScreen.VOICE_CLONE) },
                                icon = { Icon(Icons.Default.Mic, contentDescription = "Clone") },
                                label = { Text("ভয়েস ক্লোন", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.Black,
                                    selectedTextColor = NeonCyanBright,
                                    indicatorColor = NeonCyan,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.SPEECH_STUDIO,
                                onClick = { viewModel.navigateTo(AppScreen.SPEECH_STUDIO) },
                                icon = { Icon(Icons.Default.GraphicEq, contentDescription = "Studio") },
                                label = { Text("স্পিচ স্টুডিও", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = ElectricVioletBright,
                                    indicatorColor = ElectricViolet,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.VOICE_LIBRARY,
                                onClick = { viewModel.navigateTo(AppScreen.VOICE_LIBRARY) },
                                icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") },
                                label = { Text("লাইব্রেরি", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = ElectricVioletBright,
                                    indicatorColor = ElectricViolet,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )

                            NavigationBarItem(
                                selected = currentScreen == AppScreen.HISTORY,
                                onClick = { viewModel.navigateTo(AppScreen.HISTORY) },
                                icon = { Icon(Icons.Default.History, contentDescription = "History") },
                                label = { Text("ইতিহাস", fontSize = 11.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = ElectricVioletBright,
                                    indicatorColor = ElectricViolet,
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        (fadeIn() + slideInHorizontally { width -> width / 4 })
                            .togetherWith(fadeOut() + slideOutHorizontally { width -> -width / 4 })
                    },
                    label = "screen_transition"
                ) { screen ->
                    when (screen) {
                        AppScreen.HOME -> HomeScreen(viewModel = viewModel)
                        AppScreen.VOICE_CLONE -> VoiceCloneWizardScreen(viewModel = viewModel)
                        AppScreen.SPEECH_STUDIO -> SpeechStudioScreen(viewModel = viewModel)
                        AppScreen.VOICE_LIBRARY -> VoiceLibraryScreen(viewModel = viewModel)
                        AppScreen.HISTORY -> HistoryScreen(viewModel = viewModel)
                    }
                }

                // API Key Setup Dialog
                if (showApiKeyDialog) {
                    ApiKeyDialog(
                        repository = viewModel.repository,
                        onDismiss = { viewModel.setShowApiKeyDialog(false) }
                    )
                }
            }
        }
    }
}
