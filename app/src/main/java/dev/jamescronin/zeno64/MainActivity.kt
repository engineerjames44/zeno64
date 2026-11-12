package dev.jamescronin.zeno64

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

// Step 1: the smallest thing that proves the app builds, installs and runs on the TV.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFF2B2A8C), Color(0xFFE08A3C)))),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "ZENO 64",
                    style = TextStyle(color = Color(0xFFFFE14D), fontSize = 64.sp, fontFamily = FontFamily.Monospace),
                )
            }
        }
    }
}
