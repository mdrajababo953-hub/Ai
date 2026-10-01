package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SpatialAudio
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.CartesiaVoice
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.TtsUiState
import com.example.ui.components.AnimatedWaveformVisualizer
import com.example.ui.theme.CyanVioletGradient
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ElectricVioletBright
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonCyanBright
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonPink
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioCard
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.io.File
import java.util.Locale

@Composable
fun SpeechStudioScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.navigateTo(AppScreen.HOME)
    }

    val context = LocalContext.current
    val selectedVoice by viewModel.selectedVoice.collectAsState()
    val clonedVoices by viewModel.clonedVoices.collectAsState()
    val ttsState by viewModel.ttsState.collectAsState()
    val defaultVoices = viewModel.defaultSonicVoices

    val currentPlayingPath by viewModel.playerHelper.currentPlayingPath.collectAsState()
    val isPlaying by viewModel.playerHelper.isPlaying.collectAsState()
    val currentPosMs by viewModel.playerHelper.currentPositionMs.collectAsState()
    val durationMs by viewModel.playerHelper.durationMs.collectAsState()
    val playbackSpeed by viewModel.playerHelper.playbackSpeed.collectAsState()

    var inputText by remember { mutableStateOf(viewModel.ttsInputText.value) }
    var selectedLanguage by remember { mutableStateOf(viewModel.ttsSelectedLanguage.value) }
    var selectedModel by remember { mutableStateOf(viewModel.ttsSelectedModel.value) }

    val samplePrompts = listOf(
        "হ্যালো! আমি কার্তেসিয়া সোনিক এআই দিয়ে নিখুঁতভাবে কথা বলছি।",
        "আমি আপনার ক্লোন করা কণ্ঠ। আপনি এখন যা লিখবেন, আমি তাই বলে দেব!",
        "Hello! This is a real-time voice synthesis demo using Cartesia Sonic.",
        "আজকের দিনটি সত্যিই সুন্দর, সবার জীবন আনন্দে ভরে উঠুক।"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo(AppScreen.HOME) },
                    modifier = Modifier.testTag("tts_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "স্পিচ স্টুডিও (Speech Studio)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Cartesia Sonic Text-to-Speech Engine",
                        fontSize = 12.sp,
                        color = ElectricVioletBright
                    )
                }
            }
        }

        // Active Voice Card Header
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCard)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "নির্বাচিত কণ্ঠ (Active Speaker)",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (selectedVoice.isCustom) NeonCyan.copy(alpha = 0.25f) else ElectricViolet.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedVoice.isCustom) Icons.Default.RecordVoiceOver else Icons.Default.SpatialAudio,
                                    contentDescription = null,
                                    tint = if (selectedVoice.isCustom) NeonCyanBright else ElectricVioletBright,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = selectedVoice.name,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    if (selectedVoice.isCustom) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(NeonCyan.copy(alpha = 0.2f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "CLONED",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = NeonCyanBright
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = selectedVoice.description ?: "Ready to speak",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.navigateTo(AppScreen.VOICE_LIBRARY) },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("বদলান", fontSize = 11.sp, color = NeonCyan)
                        }
                    }
                }
            }
        }

        // Voice Selector Horizontal Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "দ্রুত কণ্ঠ পরিবর্তন করুন (Quick Voice Switch)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Show all cloned voices first!
                    items(clonedVoices) { cloned ->
                        val isSelected = selectedVoice.id == cloned.cartesiaVoiceId
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectClonedVoiceForTts(cloned) },
                            label = {
                                Text("🎭 ${cloned.name}", fontSize = 12.sp)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = NeonCyan,
                                selectedLabelColor = Color.Black
                            )
                        )
                    }

                    // Default Sonic voices
                    items(defaultVoices) { sonic ->
                        val isSelected = selectedVoice.id == sonic.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectVoiceForTts(sonic) },
                            label = {
                                Text("⚡ ${sonic.name}", fontSize = 12.sp)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricViolet,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // Text Input Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(20.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCard)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "কী বলবেন? (Type what to say)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (inputText.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    inputText = ""
                                    viewModel.ttsInputText.value = ""
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = {
                            inputText = it
                            viewModel.ttsInputText.value = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .testTag("input_speech_text"),
                        placeholder = {
                            Text("এখানে বাংলা বা ইংরেজিতে আপনার পছন্দের কথা লিখুন...")
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricViolet,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        )
                    )

                    // Quick prompt chips
                    Text(
                        text = "নমুনা বাক্য (Sample Prompts):",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(samplePrompts) { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(StudioBorder.copy(alpha = 0.5f))
                                    .clickable {
                                        inputText = prompt
                                        viewModel.ttsInputText.value = prompt
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = prompt,
                                    fontSize = 11.sp,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Voice Controls (Language & Model)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Language selector chips
                val langs = listOf("bn" to "বাংলা", "en" to "English", "hi" to "हिन्दी")
                langs.forEach { (code, label) ->
                    val isSel = selectedLanguage == code
                    FilterChip(
                        selected = isSel,
                        onClick = {
                            selectedLanguage = code
                            viewModel.ttsSelectedLanguage.value = code
                        },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricViolet,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
        }

        // Generate Action Button
        item {
            val isGenerating = (ttsState is TtsUiState.Generating)
            val canGenerate = inputText.isNotBlank() && !isGenerating

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { viewModel.generateSpeech() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_generate_speech"),
                    enabled = canGenerate,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricViolet,
                        disabledContainerColor = StudioBorder
                    )
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "কার্তেসিয়া দিয়ে কণ্ঠ তৈরি হচ্ছে...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "কণ্ঠ তৈরি করুন (Generate Speech)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Error message
                if (ttsState is TtsUiState.Error) {
                    val msg = (ttsState as TtsUiState.Error).message
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonPink.copy(alpha = 0.2f))
                            .border(1.dp, NeonPink.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = NeonPink
                        )
                        Text(text = msg, fontSize = 12.sp, color = NeonPink)
                    }
                }
            }
        }

        // Generated Result Player Card
        val lastGenerated = (ttsState as? TtsUiState.Success)?.generatedAudio
        if (lastGenerated != null) {
            item {
                val isThisPlaying = isPlaying && currentPlayingPath == lastGenerated.filePath

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .border(1.dp, NeonEmerald.copy(alpha = 0.7f), RoundedCornerShape(22.dp)),
                    colors = CardDefaults.cardColors(containerColor = StudioCard)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = NeonEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "অডিও সম্পূর্ণ তৈরি হয়েছে!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = NeonEmerald
                                )
                            }

                            // Share button
                            IconButton(
                                onClick = {
                                    shareGeneratedAudio(context, lastGenerated.filePath)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = NeonCyan
                                )
                            }
                        }

                        // Text quote
                        Text(
                            text = "\"${lastGenerated.textPrompt}\"",
                            fontSize = 13.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )

                        // Glowing Waveform
                        AnimatedWaveformVisualizer(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            isActive = isThisPlaying,
                            amplitude = 0.75f
                        )

                        // Seek bar & controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = formatDuration(currentPosMs),
                                fontSize = 11.sp,
                                color = TextSecondary
                            )

                            Slider(
                                value = if (durationMs > 0) currentPosMs.toFloat() / durationMs.toFloat() else 0f,
                                onValueChange = { frac ->
                                    if (durationMs > 0) {
                                        viewModel.playerHelper.seekTo((frac * durationMs).toInt())
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = NeonCyan,
                                    activeTrackColor = ElectricViolet,
                                    inactiveTrackColor = StudioBorder
                                )
                            )

                            Text(
                                text = formatDuration(durationMs),
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        // Play/Pause Big Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(CyanVioletGradient)
                                    .clickable {
                                        viewModel.playerHelper.togglePlayPause(lastGenerated.filePath)
                                    }
                                    .testTag("btn_play_generated_audio"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatDuration(millis: Int): String {
    val totalSeconds = (millis / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

private fun shareGeneratedAudio(context: Context, path: String) {
    try {
        val file = File(path)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "audio/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val shareIntent = Intent.createChooser(sendIntent, "Share Speech")
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(shareIntent)
    } catch (_: Exception) {}
}
