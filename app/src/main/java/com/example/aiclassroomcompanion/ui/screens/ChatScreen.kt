package com.example.aiclassroomcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Schedule
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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.aiclassroomcompanion.ui.theme.AICLASSROOMCOMPANIONTheme
import com.example.aiclassroomcompanion.ui.theme.Gold
import com.example.aiclassroomcompanion.ui.theme.Maroon
import java.util.*

import androidx.compose.runtime.rememberCoroutineScope
import com.example.aiclassroomcompanion.util.HuggingFaceService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(navController: NavController) {
    var messageText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val hfService = remember { HuggingFaceService() }
    
    val messages = remember { mutableStateListOf(
        ChatMessage("Hello! I am your AI Classroom Assistant. Ask me anything about your lectures, notes, or study topics!", false)
    ) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("AI Study Assistant", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("Interactive Classroom Assistant", color = Color.LightGray, fontSize = 12.sp)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Back", tint = Gold)
                    }
                },
                actions = {
                    IconButton(onClick = { messages.clear(); messages.add(ChatMessage("Hello! How can I help you with your studies today?", false)) }) {
                        Icon(Icons.Default.Schedule, contentDescription = "Clear History", tint = Gold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Surface(
                color = Maroon,
                modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = messageText,
                        onValueChange = { messageText = it },
                        placeholder = { Text("Ask anything about your lecture...", color = Color.Gray, fontSize = 14.sp) },
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                    IconButton(
                        onClick = { 
                            val query = messageText.trim()
                            if (query.isNotBlank()) {
                                messages.add(ChatMessage(query, true))
                                messageText = ""
                                scope.launch {
                                    val aiResponse = hfService.generateText("You are an expert classroom assistant. Answer this student query clearly and concisely: $query")
                                        ?: "Here is a breakdown for '$query': It is an important concept in your lecture. Review your generated notes and summary for additional details!"
                                    messages.add(ChatMessage(aiResponse, false))
                                }
                            }
                        },
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Gold)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Maroon, modifier = Modifier.size(20.dp))
                    }
                }
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
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(messages) { message ->
                    ChatBubble(message)
                }
            }
        }
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun ChatBubble(message: ChatMessage) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            color = if (message.isUser) Gold else Color.White.copy(alpha = 0.05f),
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 0.dp,
                bottomEnd = if (message.isUser) 0.dp else 16.dp
            )
        ) {
            Text(
                text = message.text,
                color = if (message.isUser) Maroon else Color.White,
                modifier = Modifier.padding(12.dp),
                fontSize = 14.sp
            )
        }
    }
}

@Preview
@Composable
fun ChatScreenPreview() {
    AICLASSROOMCOMPANIONTheme {
        ChatScreen(rememberNavController())
    }
}
