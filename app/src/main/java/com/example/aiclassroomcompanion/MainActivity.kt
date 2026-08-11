package com.example.aiclassroomcompanion

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import com.example.aiclassroomcompanion.ui.MainScreen
import com.example.aiclassroomcompanion.ui.theme.AICLASSROOMCOMPANIONTheme

class MainActivity : ComponentActivity() {

    // Step 2: Request Runtime Permission
    private val permissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                Toast.makeText(this, "Audio Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Audio Permission is required for recording", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        com.example.aiclassroomcompanion.util.LocalLectureStore.init(applicationContext)

        // Launch permission request on startup as requested in guide
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)

        enableEdgeToEdge()
        setContent {
            AICLASSROOMCOMPANIONTheme {
                MainScreen()
            }
        }
    }
}
