package com.example.aiclassroomcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.aiclassroomcompanion.ui.viewmodels.AIState
import com.example.aiclassroomcompanion.ui.viewmodels.LectureViewModel

import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(navController: NavController, transcription: String, viewModel: LectureViewModel = viewModel()) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val summaryState by viewModel.summaryState.collectAsState()

    LaunchedEffect(Unit) {
        if (summaryState is AIState.Idle) {
            viewModel.generateSummary(transcription)
        }
    }
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Summary", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Gold)
                    }
                },
                actions = {
                    IconButton(onClick = { /* AI Info */ }) {
                        Icon(Icons.Default.AutoAwesome, "AI", tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF1A0000),
                contentColor = Gold,
                modifier = Modifier.height(70.dp)
            ) {
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        val text = (summaryState as? AIState.Success)?.result ?: ""
                        if (text.isNotEmpty()) {
                            clipboardManager.setText(AnnotatedString(text))
                            Toast.makeText(context, "Summary copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    },
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") },
                    label = { Text("Copy", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Gold, unselectedTextColor = Gold)
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        val text = (summaryState as? AIState.Success)?.result ?: ""
                        if (text.isNotEmpty()) {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Summary"))
                        }
                    },
                    icon = { Icon(Icons.Default.Share, contentDescription = "Share") },
                    label = { Text("Share", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Gold, unselectedTextColor = Gold)
                )
                NavigationBarItem(
                    selected = false,
                    onClick = {
                        val text = (summaryState as? AIState.Success)?.result ?: ""
                        if (text.isNotEmpty()) {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, text)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Summary"))
                        }
                    },
                    icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF") },
                    label = { Text("Export", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Gold, unselectedTextColor = Gold)
                )
            }
        }
    ) { paddingValues ->
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when (val state = summaryState) {
                is AIState.Processing -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Gold)
                    }
                }
                is AIState.Success -> {
                    Text(
                        text = "AI Generated Summary",
                        color = Gold,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = state.result,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp),
                            fontSize = 14.sp
                        )
                    }
                }
                is AIState.Error -> {
                    Text(text = "Error: ${state.message}", color = Color.Red)
                }
                else -> {}
            }
        }
    }
}

@Preview
@Composable
fun SummaryScreenPreview() {
    AICLASSROOMCOMPANIONTheme {
        SummaryScreen(rememberNavController(), transcription = "")
    }
}
