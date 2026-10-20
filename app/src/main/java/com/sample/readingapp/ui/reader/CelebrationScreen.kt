package com.sample.readingapp.ui.reader

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sample.readingapp.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ---------------- CELEBRATION SCREEN ----------------
@Composable
fun CelebrationScreen(
    username: String,
    highlightColor: Color,
    onFinished: () -> Unit
) {
    val scale = remember { Animatable(0.4f) }
    val alpha = remember { Animatable(0f) }

    val appreciationPhrases = remember {
        listOf(
            "Spectacular Fluency, $username!",
            "Brilliant Reading, $username!",
            "Magnificent Job, $username!",
            "Extraordinary Effort, $username!",
            "Superstar Reader, $username!",
            "Phenomenal Reading, $username!",
            "Remarkable Journey, $username!"
        )
    }
    val chosenAppreciation = remember { appreciationPhrases.random() }

    LaunchedEffect(Unit) {
        launch { alpha.animateTo(1f, tween(300)) }
        scale.animateTo(
            1.15f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        )
        delay(1900)
        launch { alpha.animateTo(0f, tween(400)) }
        delay(400)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        highlightColor.copy(alpha = 0.65f),
                        BrandShadeDeep
                    ),
                    radius = 1200f
                )
            )
            .clickable { onFinished() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .padding(28.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                    this.alpha = alpha.value
                }
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(highlightColor.copy(alpha = 0.35f), CircleShape)
                    .border(3.dp, highlightColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(54.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(30.dp))
                Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(34.dp))
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BrandShadeMedium, modifier = Modifier.size(30.dp))
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Chapter Complete!",
                fontSize = 34.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = FontFamily.Serif,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.5.dp, highlightColor.copy(alpha = 0.75f)),
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                Text(
                    text = chosenAppreciation,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Loading your review & quiz...",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
