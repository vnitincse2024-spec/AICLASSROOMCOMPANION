package com.example.aiclassroomcompanion.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.aiclassroomcompanion.ui.theme.AICLASSROOMCOMPANIONTheme
import com.example.aiclassroomcompanion.ui.theme.Gold
import com.example.aiclassroomcompanion.ui.theme.Maroon
import com.example.aiclassroomcompanion.ui.viewmodels.AIState
import com.example.aiclassroomcompanion.ui.viewmodels.LectureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashcardsScreen(navController: NavController, transcription: String, viewModel: LectureViewModel = viewModel()) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val flashcardsState by viewModel.flashcardsState.collectAsState()

    LaunchedEffect(Unit) {
        if (flashcardsState is AIState.Idle) {
            viewModel.generateFlashcards(transcription)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Flashcards", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Gold)
                    }
                },
                actions = {
                    IconButton(onClick = { /* Add flashcard */ }) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = Gold)
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
                    selected = true,
                    onClick = { /* TODO */ },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Cards") },
                    label = { Text("Cards") },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Gold, selectedTextColor = Gold)
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* TODO */ },
                    icon = { Icon(Icons.Default.Add, contentDescription = "List") },
                    label = { Text("List") },
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = Gold, unselectedTextColor = Gold)
                )
                NavigationBarItem(
                    selected = false,
                    onClick = { /* TODO */ },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Progress") },
                    label = { Text("Progress") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = flashcardsState) {
                is AIState.Processing -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Gold)
                    }
                }
                is AIState.FlashcardsSuccess -> {
                    val flashcards = state.flashcards
                    if (flashcards.isNotEmpty()) {
                        Text(
                            text = "Q${currentIndex + 1} / ${flashcards.size}",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        FlashcardItem(flashcards[currentIndex])

                        Spacer(modifier = Modifier.height(48.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { if (currentIndex > 0) currentIndex-- },
                                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev", tint = Gold)
                            }
                            
                            Button(
                                onClick = { /* Flip is handled by card itself */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(56.dp).width(150.dp)
                            ) {
                                Text("Show Answer", color = Maroon, fontWeight = FontWeight.Bold)
                            }
                            
                            IconButton(
                                onClick = { if (currentIndex < flashcards.size - 1) currentIndex++ },
                                modifier = Modifier.size(56.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f))
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next", tint = Gold)
                            }
                        }
                    } else {
                        Text(text = "No flashcards generated", color = Color.White)
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

data class Flashcard(val question: String, val answer: String)

@Composable
fun FlashcardItem(flashcard: Flashcard) {
    var rotated by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (rotated) 180f else 0f,
        animationSpec = tween(durationMillis = 500),
        label = "rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp)
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 8 * density
            }
            .clickable { rotated = !rotated },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF800000),
                            Color(0xFF4A0000)
                        )
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (rotation <= 90f) {
                Text(
                    text = flashcard.question,
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = flashcard.answer,
                    color = Gold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.graphicsLayer { rotationY = 180f }
                )
            }
        }
    }
}

@Preview
@Composable
fun FlashcardsScreenPreview() {
    AICLASSROOMCOMPANIONTheme {
        FlashcardsScreen(rememberNavController(), transcription = "")
    }
}
