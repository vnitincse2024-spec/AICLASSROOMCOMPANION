package com.example.aiclassroomcompanion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.aiclassroomcompanion.ui.theme.Gold

@Composable
fun AppLogo(size: Dp = 100.dp) {
    Box(
        modifier = Modifier
            .size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.toPx()
            val canvasHeight = size.toPx()
            
            // Path for the central building
            val centerBuildingPath = Path().apply {
                moveTo(canvasWidth * 0.35f, canvasHeight * 0.85f)
                lineTo(canvasWidth * 0.35f, canvasHeight * 0.45f)
                lineTo(canvasWidth * 0.5f, canvasHeight * 0.25f)
                lineTo(canvasWidth * 0.65f, canvasHeight * 0.45f)
                lineTo(canvasWidth * 0.65f, canvasHeight * 0.85f)
                
                // Door arch
                arcTo(
                    rect = Rect(
                        left = canvasWidth * 0.44f,
                        top = canvasHeight * 0.72f,
                        right = canvasWidth * 0.56f,
                        bottom = canvasHeight * 0.85f
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                close()
            }
            
            // Left building
            val leftBuildingPath = Path().apply {
                moveTo(canvasWidth * 0.15f, canvasHeight * 0.85f)
                lineTo(canvasWidth * 0.15f, canvasHeight * 0.55f)
                lineTo(canvasWidth * 0.32f, canvasHeight * 0.55f)
                lineTo(canvasWidth * 0.32f, canvasHeight * 0.85f)
                close()
            }
            
            // Right building
            val rightBuildingPath = Path().apply {
                moveTo(canvasWidth * 0.68f, canvasHeight * 0.85f)
                lineTo(canvasWidth * 0.68f, canvasHeight * 0.55f)
                lineTo(canvasWidth * 0.85f, canvasHeight * 0.55f)
                lineTo(canvasWidth * 0.85f, canvasHeight * 0.85f)
                close()
            }
            
            drawPath(path = centerBuildingPath, color = Gold)
            drawPath(path = leftBuildingPath, color = Gold)
            drawPath(path = rightBuildingPath, color = Gold)
            
            // Windows for left building
            val windowSize = canvasWidth * 0.05f
            for (i in 0..2) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(canvasWidth * 0.21f, canvasHeight * (0.6f + i * 0.08f)),
                    size = Size(windowSize, windowSize)
                )
            }
            
            // Windows for right building
            for (i in 0..2) {
                drawRect(
                    color = Color.Black.copy(alpha = 0.5f),
                    topLeft = Offset(canvasWidth * 0.74f, canvasHeight * (0.6f + i * 0.08f)),
                    size = Size(windowSize, windowSize)
                )
            }
        }
    }
}
