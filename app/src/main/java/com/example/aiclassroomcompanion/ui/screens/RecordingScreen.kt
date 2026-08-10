package com.example.aiclassroomcompanion.ui.screens

import android.Manifest
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.aiclassroomcompanion.ui.theme.AICLASSROOMCOMPANIONTheme
import com.example.aiclassroomcompanion.ui.theme.Gold
import com.example.aiclassroomcompanion.ui.theme.Maroon
import com.example.aiclassroomcompanion.ui.viewmodels.RecordingViewModel
import com.example.aiclassroomcompanion.ui.viewmodels.UploadState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun RecordingScreen(navController: NavController, viewModel: RecordingViewModel = viewModel()) {
    val isRecording by viewModel.isRecording.collectAsState()
    val seconds by viewModel.seconds.collectAsState()
    val transcription by viewModel.transcription.collectAsState()
    val partialText by viewModel.partialText.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val uploadState by viewModel.uploadState.collectAsState()
    
    var showSaveDialog by remember { mutableStateOf(false) }
    var lectureTitle by remember { mutableStateOf("") }
    
    val permissionState = rememberPermissionState(permission = Manifest.permission.RECORD_AUDIO)

    LaunchedEffect(uploadState) {
        if (uploadState is UploadState.Success) {
            navController.popBackStack()
        }
    }

    LaunchedEffect(permissionState.status) {
        if (!permissionState.status.isGranted) {
            permissionState.launchPermissionRequest()
        }
    }

    // Pulse animation for the mic
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val timeString = String.format(Locale.getDefault(), "%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recording Lecture", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (isRecording) viewModel.stopRecording()
                        navController.popBackStack() 
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel", tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { paddingValues ->
        if (showSaveDialog) {
            AlertDialog(
                onDismissRequest = { showSaveDialog = false },
                title = { Text("Save Lecture") },
                text = {
                    OutlinedTextField(
                        value = lectureTitle,
                        onValueChange = { lectureTitle = it },
                        label = { Text("Lecture Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { 
                            viewModel.stopAndSaveRecording(lectureTitle)
                            showSaveDialog = false
                        },
                        enabled = lectureTitle.isNotBlank()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (uploadState is UploadState.Uploading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Gold)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF4A0000),
                            Color(0xFF1A0000)
                        )
                    )
                )
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (lectureTitle.isNotBlank()) lectureTitle else "New Classroom Recording",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isRecording) "Live Transcription in progress..." else "Recording Paused",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            }

            // Animated Mic Section
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
                if (isRecording) {
                    val volumeScale = (1f + (volume.coerceIn(0f, 10f) / 10f)).coerceIn(1f, 2f)
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .scale(volumeScale * scale)
                            .clip(CircleShape)
                            .background(Gold.copy(alpha = 0.2f))
                    )
                }
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Gold),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Maroon
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeString,
                    color = Color.White,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Light
                )
                Spacer(modifier = Modifier.height(32.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start/Stop Button
                    IconButton(
                        onClick = { 
                            if (isRecording) viewModel.stopRecording() else viewModel.startRecording()
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Pause else Icons.Default.Mic,
                            contentDescription = if (isRecording) "Pause" else "Record",
                            tint = Gold,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Stop/Save Button
                    IconButton(
                        onClick = { 
                            showSaveDialog = true
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.Red)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
            
            // Transcription Preview
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                    val displayText = buildString {
                        if (transcription.isNotEmpty()) append(transcription.trim())
                        if (partialText.isNotEmpty()) {
                            if (isNotEmpty()) append(" ")
                            append(partialText)
                        }
                    }
                    
                    Text(
                        text = if (displayText.isNotEmpty()) "\"$displayText...\""
                               else if (isRecording) "\"Listening for audio...\""
                               else "\"Transcription paused...\"",
                        color = Color.LightGray.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun RecordingScreenPreview() {
    AICLASSROOMCOMPANIONTheme {
        RecordingScreen(rememberNavController())
    }
}
