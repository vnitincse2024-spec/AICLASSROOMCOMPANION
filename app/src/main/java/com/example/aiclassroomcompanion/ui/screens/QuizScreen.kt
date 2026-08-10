package com.example.aiclassroomcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.aiclassroomcompanion.ui.viewmodels.AIState
import com.example.aiclassroomcompanion.ui.viewmodels.LectureViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(navController: NavController, transcription: String, viewModel: LectureViewModel = viewModel()) {
    var selectedOption by remember { mutableStateOf<Int?>(null) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    val quizState by viewModel.quizState.collectAsState()

    LaunchedEffect(Unit) {
        if (quizState is AIState.Idle) {
            viewModel.generateQuiz(transcription)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Quiz", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
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
                .padding(24.dp)
        ) {
            when (val state = quizState) {
                is AIState.Processing -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Gold)
                    }
                }
                is AIState.QuizSuccess -> {
                    val questions = state.questions
                    if (questions.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Tab(selected = true, onClick = {}, text = { Text("MCQ", color = Gold) })
                            Tab(selected = false, onClick = {}, text = { Text("Short Answer", color = Color.Gray) })
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = "Q${currentQuestionIndex + 1}. ${questions[currentQuestionIndex].text}",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        questions[currentQuestionIndex].options.forEachIndexed { index, option ->
                            OptionItem(
                                text = "${('A' + index)}. $option",
                                isSelected = selectedOption == index,
                                onClick = { selectedOption = index }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        
                        Spacer(modifier = Modifier.weight(1f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentQuestionIndex + 1} / ${questions.size}",
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            
                            Button(
                                onClick = { 
                                    if (currentQuestionIndex < questions.size - 1) {
                                        currentQuestionIndex++
                                        selectedOption = null
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.height(56.dp).width(120.dp)
                            ) {
                                Text("Next", color = Maroon, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Maroon)
                            }
                        }
                    } else {
                        Text(text = "No questions generated", color = Color.White)
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

data class Question(val text: String, val options: List<String>, val correctAnswer: Int)

@Composable
fun OptionItem(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent)
            .border(
                width = 2.dp,
                color = if (isSelected) Gold else Color.White.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .border(2.dp, if (isSelected) Gold else Color.White, RoundedCornerShape(10.dp))
                    .background(if (isSelected) Gold else Color.Transparent)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = text, color = Color.White, fontSize = 16.sp)
        }
    }
}

@Preview
@Composable
fun QuizScreenPreview() {
    AICLASSROOMCOMPANIONTheme {
        QuizScreen(rememberNavController(), transcription = "")
    }
}
