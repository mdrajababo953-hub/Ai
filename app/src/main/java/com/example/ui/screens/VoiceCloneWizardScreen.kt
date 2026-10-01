package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.AppScreen
import com.example.ui.CloneUiState
import com.example.ui.MainViewModel
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
import java.io.FileOutputStream

@Composable
fun VoiceCloneWizardScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.resetCloneWizard()
        viewModel.navigateTo(AppScreen.HOME)
    }

    val context = LocalContext.current
    val cloneState by viewModel.cloneState.collectAsState()
    val isRecording by viewModel.recorderHelper.isRecording.collectAsState()
    val recordingDuration by viewModel.recorderHelper.recordingDurationSec.collectAsState()
    val micAmplitude by viewModel.recorderHelper.amplitudeLevel.collectAsState()

    val currentPlayingPath by viewModel.playerHelper.currentPlayingPath.collectAsState()
    val isPlayingPreview by viewModel.playerHelper.isPlaying.collectAsState()

    var voiceName by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf("bn") }
    var voiceDesc by remember { mutableStateOf("") }

    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMicPermission = granted
        if (granted) {
            viewModel.startRecording()
        }
    }

    // Audio file picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val copiedFile = copyUriToInternalFile(context, it)
            if (copiedFile != null) {
                viewModel.setCustomAudioFile(copiedFile)
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse_mic")
    val micPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_scale"
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Navigation Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        viewModel.resetCloneWizard()
                        viewModel.navigateTo(AppScreen.HOME)
                    },
                    modifier = Modifier.testTag("clone_back_button")
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
                        text = "ভয়েস ক্লোন স্টুডিও (Voice Clone)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Instant 30s-60s Audio Voice Cloning",
                        fontSize = 12.sp,
                        color = NeonCyanBright
                    )
                }
            }
        }

        // Instructions banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp)),
                colors = CardDefaults.cardColors(containerColor = StudioCard)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Audiotrack,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "৩০ থেকে ৬০ সেকেন্ড পরিষ্কার অডিও দিন",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "কার্তেসিয়ার Sonic মডেল দিয়ে কণ্ঠের উচ্চারণ ও এক্সপ্রেশন নিখুঁতভাবে কপি করা হবে।",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // STEP 1: RECORD OR UPLOAD AUDIO
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
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "ধাপ ১: ভয়েস স্যাম্পল সংগ্রহ করুন",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio Waveform Visualizer
                    AnimatedWaveformVisualizer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        isActive = isRecording || isPlayingPreview,
                        amplitude = if (isRecording) micAmplitude else 0.6f
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Timer Display
                    val timerText = if (isRecording) {
                        val min = recordingDuration / 60
                        val sec = recordingDuration % 60
                        String.format("%02d:%02d / 01:00", min, sec)
                    } else if (cloneState is CloneUiState.Recorded) {
                        "অডিও রেকর্ড সম্পন্ন হয়েছে! (Audio Ready)"
                    } else {
                        "রেকর্ড করতে মাইক্রোফোন চাপুন (৩০-৬০ সেকেন্ড)"
                    }

                    Text(
                        text = timerText,
                        fontSize = 14.sp,
                        fontWeight = if (isRecording) FontWeight.Bold else FontWeight.Normal,
                        color = if (isRecording) NeonPink else NeonCyanBright
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Main Big Record Button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (isRecording) {
                            // Stop recording button
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(NeonPink)
                                    .clickable {
                                        viewModel.stopRecording()
                                    }
                                    .testTag("btn_stop_recording"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Stop,
                                    contentDescription = "Stop Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        } else {
                            // Start Recording Button
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(CyanVioletGradient)
                                    .clickable {
                                        if (hasMicPermission) {
                                            viewModel.startRecording()
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                    .testTag("btn_start_recording"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = "Start Recording",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // If audio is already recorded/picked, show preview playback
                    val recordedFile = (cloneState as? CloneUiState.Recorded)?.audioFile
                    if (recordedFile != null) {
                        val isPlayingThis = isPlayingPreview && currentPlayingPath == recordedFile.absolutePath

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(NeonEmerald)
                                        .clickable {
                                            viewModel.playerHelper.togglePlayPause(recordedFile.absolutePath)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingThis) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = "Preview Audio",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "নমুনা অডিও প্রিভিউ শুনুন",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = recordedFile.name,
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            IconButton(onClick = { viewModel.resetCloneWizard() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reset Recording",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Upload Audio File button
                    OutlinedButton(
                        onClick = { filePickerLauncher.launch("audio/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .testTag("btn_upload_audio_file"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = NeonCyan)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Upload Audio",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "অথবা ডিভাইস থেকে অডিও ফাইল সিলেক্ট করুন",
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // STEP 2: VOICE DETAILS (Name & Language)
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
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "ধাপ ২: ক্লোন করা কণ্ঠের বিবরণ",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    OutlinedTextField(
                        value = voiceName,
                        onValueChange = {
                            voiceName = it
                            viewModel.cloneVoiceName.value = it
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("input_voice_name"),
                        label = { Text("কণ্ঠের নাম দিন (Voice Name)") },
                        placeholder = { Text("যেমন: আমার নিজস্ব কণ্ঠ / Rahim Voice") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        )
                    )

                    Text(
                        text = "ভাষা নির্বাচন করুন (Language)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val languages = listOf("bn" to "বাংলা", "en" to "English", "hi" to "हिन्दी")
                        languages.forEach { (code, label) ->
                            val isSelected = selectedLanguage == code
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedLanguage = code
                                    viewModel.cloneVoiceLanguage.value = code
                                },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = ElectricViolet,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    OutlinedTextField(
                        value = voiceDesc,
                        onValueChange = {
                            voiceDesc = it
                            viewModel.cloneVoiceDescription.value = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("বিবরণ (ঐচ্ছিক)") },
                        placeholder = { Text("যেমন: শান্ত ও সাবলীল কণ্ঠ") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = StudioBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            cursorColor = NeonCyan
                        )
                    )
                }
            }
        }

        // STEP 3: SUBMIT CLONE ACTION & STATES
        item {
            val isAudioReady = (cloneState is CloneUiState.Recorded)
            val isCloning = (cloneState is CloneUiState.Cloning)

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Error message display
                if (cloneState is CloneUiState.Error) {
                    val errMsg = (cloneState as CloneUiState.Error).message
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
                        Text(
                            text = errMsg,
                            fontSize = 12.sp,
                            color = NeonPink
                        )
                    }
                }

                // Success message display
                if (cloneState is CloneUiState.Success) {
                    val cloned = (cloneState as CloneUiState.Success).clonedVoice
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, NeonEmerald, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = StudioCard)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = NeonEmerald,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "ভয়েস ক্লোন সফল হয়েছে! 🎉",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "কণ্ঠ '${cloned.name}' এখন স্পিচ স্টুডিওতে যুক্ত হয়েছে। আপনি যা লিখবেন তাই এই কণ্ঠে শোনা যাবে!",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                            )

                            Button(
                                onClick = {
                                    viewModel.selectClonedVoiceForTts(cloned)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
                            ) {
                                Text(
                                    text = "এই কণ্ঠে কথা বলতে যান (Generate Speech)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                }

                // Main Clone Action Button
                if (cloneState !is CloneUiState.Success) {
                    Button(
                        onClick = {
                            viewModel.executeVoiceClone()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("btn_execute_clone"),
                        enabled = isAudioReady && !isCloning,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = NeonCyan,
                            disabledContainerColor = StudioBorder
                        )
                    ) {
                        if (isCloning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.Black,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "কার্তেসিয়া দিয়ে ভয়েস ক্লোন করা হচ্ছে...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        } else {
                            Text(
                                text = if (isAudioReady) "ভয়েস ক্লোন সম্পন্ন করুন (Create Voice Clone)" else "প্রথমে অডিও রেকর্ড বা আপলোড করুন",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isAudioReady) Color.Black else TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun copyUriToInternalFile(context: Context, uri: Uri): File? {
    return try {
        val resolver = context.contentResolver
        val inputStream = resolver.openInputStream(uri) ?: return null
        val cacheDir = File(context.cacheDir, "imported_audio")
        if (!cacheDir.exists()) cacheDir.mkdirs()
        val file = File(cacheDir, "picked_${System.currentTimeMillis()}.m4a")
        FileOutputStream(file).use { out ->
            inputStream.copyTo(out)
        }
        file
    } catch (_: Exception) {
        null
    }
}
