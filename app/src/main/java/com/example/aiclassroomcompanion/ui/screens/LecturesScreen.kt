package com.example.aiclassroomcompanion.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.aiclassroomcompanion.ui.Screen
import com.example.aiclassroomcompanion.ui.components.LectureItem
import com.example.aiclassroomcompanion.ui.theme.AICLASSROOMCOMPANIONTheme
import com.example.aiclassroomcompanion.ui.theme.Gold
import com.example.aiclassroomcompanion.ui.theme.Maroon
import com.example.aiclassroomcompanion.ui.viewmodels.LibraryState
import com.example.aiclassroomcompanion.ui.viewmodels.LibraryViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LecturesScreen(navController: NavController, viewModel: LibraryViewModel = viewModel()) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    
    val tabs = listOf("All", "Recorded", "Uploaded")
    val libraryState by viewModel.libraryState.collectAsState()

    // Re-load lectures every time this screen becomes RESUMED (e.g. when returning from RecordingScreen)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.loadLectures()
        }
    }

    Scaffold(
        topBar = {
            if (isSearchActive) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { isSearchActive = false },
                    active = true,
                    onActiveChange = { isSearchActive = it },
                    placeholder = { Text("Search lectures...") },
                    leadingIcon = { IconButton(onClick = { isSearchActive = false }) { Icon(Icons.Default.ArrowBackIosNew, contentDescription = null, tint = Gold) } },
                    trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Default.Close, contentDescription = null, tint = Gold) } },
                    colors = SearchBarDefaults.colors(containerColor = Maroon, dividerColor = Gold),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    // Search results can be shown here if needed while typing
                }
            } else {
                CenterAlignedTopAppBar(
                    title = { Text("My Lectures", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBackIosNew, "Back", tint = Gold)
                        }
                    },
                    actions = {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Default.Search, "Search", tint = Gold)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    )
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { navController.navigate(Screen.Recording.route) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF800000)),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Gold)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Record New Lecture", color = Color.White)
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (val state = libraryState) {
                    is LibraryState.Loading -> {
                        item {
                            Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Gold)
                            }
                        }
                    }
                    is LibraryState.Success -> {
                        val tabFiltered = when (selectedTab) {
                            1 -> state.lectures.filter { it.type.isEmpty() || it.type.equals("Recorded", ignoreCase = true) }
                            2 -> state.lectures.filter { it.type.equals("Uploaded", ignoreCase = true) }
                            else -> state.lectures
                        }

                        val filteredLectures = if (searchQuery.isEmpty()) {
                            tabFiltered
                        } else {
                            tabFiltered.filter { it.title.contains(searchQuery, ignoreCase = true) }
                        }
                        
                        if (filteredLectures.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (selectedTab == 1) "No recorded lectures yet"
                                               else if (selectedTab == 2) "No uploaded lectures yet"
                                               else "No lectures available",
                                        color = Color.LightGray.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        } else {
                            items(filteredLectures) { lecture ->
                                val dateString = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(lecture.date.toDate())
                                Surface(
                                    onClick = { 
                                        navController.navigate(Screen.Notes.createRoute(lecture.transcription.ifEmpty { "Default transcription" }))
                                    },
                                    color = Color.Transparent
                                ) {
                                    LectureItem(lecture.title, "$dateString • ${lecture.duration}")
                                }
                            }
                        }
                    }
                    is LibraryState.Error -> {
                        item {
                            Text(text = state.message, color = Color.Red, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun LecturesScreenPreview() {
    AICLASSROOMCOMPANIONTheme {
        LecturesScreen(rememberNavController())
    }
}
