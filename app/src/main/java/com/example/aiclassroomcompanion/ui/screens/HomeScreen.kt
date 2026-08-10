package com.example.aiclassroomcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.aiclassroomcompanion.ui.Screen
import com.example.aiclassroomcompanion.ui.components.LectureItem
import com.example.aiclassroomcompanion.ui.theme.AICLASSROOMCOMPANIONTheme
import com.example.aiclassroomcompanion.ui.theme.Gold
import com.example.aiclassroomcompanion.ui.theme.Maroon
import com.example.aiclassroomcompanion.ui.viewmodels.LibraryViewModel
import com.example.aiclassroomcompanion.util.Lecture

@Composable
fun HomeScreen(navController: NavController, viewModel: LibraryViewModel = viewModel()) {
    val recentLecture by viewModel.recentLecture.collectAsState()
    
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
            .padding(16.dp)
    ) {
        HeaderSection()
        Spacer(modifier = Modifier.height(24.dp))
        RecordLectureCard(navController)
        Spacer(modifier = Modifier.height(24.dp))
        QuickActionsSection(navController, recentLecture)
        Spacer(modifier = Modifier.height(24.dp))
        RecentLecturesSection(navController, recentLecture)
    }
}

@Composable
fun HeaderSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Hello, Nitin 👋",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Let's make learning easier today!",
                color = Color.LightGray,
                fontSize = 14.sp
            )
        }
        IconButton(
            onClick = { /* TODO */ },
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.1f))
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Notifications",
                tint = Gold
            )
        }
    }
}

@Composable
fun RecordLectureCard(navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF800000),
                            Color(0xFF4A0000)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { navController.navigate(Screen.Recording.route) },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(Gold)
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Record",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Record Lecture",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Tap to start recording",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun QuickActionsSection(navController: NavController, recentLecture: Lecture?) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Quick Actions",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            TextButton(onClick = { navController.navigate(Screen.Lectures.route) }) {
                Text(text = "View All", color = Gold)
            }
        }
        
        val actions = listOf(
            QuickActionItem("Notes", Icons.Default.Description),
            QuickActionItem("Summary", Icons.Default.Summarize),
            QuickActionItem("Flashcards", Icons.Default.Style),
            QuickActionItem("Quiz", Icons.Default.Quiz),
            QuickActionItem("Translate", Icons.Default.Translate),
            QuickActionItem("Search", Icons.Default.Search),
            QuickActionItem("AI Chat", Icons.AutoMirrored.Filled.Chat),
            QuickActionItem("Export PDF", Icons.Default.PictureAsPdf)
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(200.dp),
            contentPadding = PaddingValues(top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(actions) { action ->
                QuickActionIcon(action) {
                    val transcription = recentLecture?.transcription ?: ""
                    when (action.name) {
                        "Notes" -> navController.navigate(Screen.Notes.createRoute(transcription.ifEmpty { "no_data" }))
                        "Summary" -> navController.navigate(Screen.Summary.createRoute(transcription.ifEmpty { "no_data" }))
                        "Flashcards" -> navController.navigate(Screen.Flashcards.createRoute(transcription.ifEmpty { "no_data" }))
                        "Quiz" -> navController.navigate(Screen.Quiz.createRoute(transcription.ifEmpty { "no_data" }))
                        "AI Chat" -> navController.navigate(Screen.Chat.route)
                    }
                }
            }
        }
    }
}

data class QuickActionItem(val name: String, val icon: ImageVector)

@Composable
fun QuickActionIcon(item: QuickActionItem, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(60.dp)
    ) {
        Surface(
            onClick = onClick,
            color = Color.White.copy(alpha = 0.1f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.size(50.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.name,
                    tint = Gold,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.name,
            color = Color.White,
            fontSize = 10.sp,
            maxLines = 1
        )
    }
}

@Composable
fun RecentLecturesSection(navController: NavController, recentLecture: Lecture?) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Lectures",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            TextButton(onClick = { navController.navigate(Screen.Lectures.route) }) {
                Text(text = "View All", color = Gold)
            }
        }
        
        if (recentLecture != null) {
            Surface(
                onClick = { navController.navigate(Screen.Notes.createRoute(recentLecture.transcription)) },
                color = Color.Transparent
            ) {
                LectureItem(recentLecture.title, "Recent • ${recentLecture.duration}")
            }
        } else {
            Text(text = "No recent lectures", color = Color.Gray, fontSize = 14.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    AICLASSROOMCOMPANIONTheme {
        HomeScreen(rememberNavController())
    }
}
