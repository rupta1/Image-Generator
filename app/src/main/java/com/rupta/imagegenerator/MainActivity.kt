package com.rupta.imagegenerator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import com.rupta.imagegenerator.ui.ImageGeneratorApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                primary = Color(0xFFB7A2FF), secondary = Color(0xFF8B78F6),
                background = Color(0xFF0B0B10), surface = Color(0xFF15151D)
            )) { ImageGeneratorApp() }
        }
    }
}
