package com.example.aiclassroomcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(navController: NavController, transcription: String, viewModel: LectureViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    
    val tabs = listOf("AI Notes", "My Notes")
    val notesState by viewModel.notesState.collectAsState()
    
    val languages = listOf("Spanish", "French", "German", "Hindi", "Japanese", "Telugu")

    // Simulate generation for demo purposes if idle
    LaunchedEffect(Unit) {
        if (notesState is AIState.Idle && transcription.isNotBlank() && transcription != "no_data") {
            viewModel.generateNotes(transcription)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Notes", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Gold)
                    }
                },
                actions = {
                    IconButton(onClick = { showLanguageDialog = true }) {
                        Icon(Icons.Default.Translate, "Translate", tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
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
                    onClick = { /* TODO */ },
                    icon = { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") },
                    label = { Text("Copy", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Gold, unselectedTextColor = Gold)
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* TODO */ },
                    icon = { Icon(Icons.Default.Share, contentDescription = "Share") },
                    label = { Text("Share", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Gold, unselectedTextColor = Gold)
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* TODO */ },
                    icon = { Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF") },
                    label = { Text("Export PDF", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Gold, unselectedTextColor = Gold)
                )
            }
        }
    ) { paddingValues ->
        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                title = { Text("Translate Notes") },
                text = {
                    Column {
                        languages.forEach { language ->
                            TextButton(
                                onClick = {
                                    val currentText = (notesState as? AIState.Success)?.result ?: ""
                                    if (currentText.isNotEmpty()) {
                                        viewModel.translateContent(currentText, language)
                                    }
                                    showLanguageDialog = false
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(language, color = Color.Black)
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
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
        ) {
            SecondaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = Gold,
                divider = {},
                indicator = {
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(selectedTab),
                        color = Gold
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, color = if (selectedTab == index) Gold else Color.Gray) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                when (val state = notesState) {
                    is AIState.Processing -> {
                        Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Gold)
                        }
                    }
                    is AIState.Success -> {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f)),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Lecture Content",
                                    color = Gold,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = state.result.replace("# ", "").replace("## ", "\n"),
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            }
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
}

@Preview
@Composable
fun NotesScreenPreview() {
    AICLASSROOMCOMPANIONTheme {
        NotesScreen(rememberNavController(), transcription = "")
    }
}
