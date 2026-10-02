package com.example.moduletwo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.moduletwo.ui.theme.ModuleTwoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ModuleTwoTheme {
                AppScreen()
            }
        }
    }
}

@Composable
private fun AppScreen() {
    Column(modifier = Modifier.fillMaxSize()) {
        CompactTopBar()
        Mainpage(
            modifier = Modifier
                .weight(1f)
                .navigationBarsPadding()
        )
    }
}

@Composable
private fun CompactTopBar() {
    Surface(
        color = Color(0xFF464646),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "WSJ3 note app",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily(Font(com.example.moduletwo.R.font.goldpaly))
            )
        }
    }
}