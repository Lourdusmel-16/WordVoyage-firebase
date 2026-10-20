package com.sample.readingapp.ui.intro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sample.readingapp.data.*
import com.sample.readingapp.theme.*
import com.sample.readingapp.ui.components.*
import com.sample.readingapp.util.*

// ---------------- WELCOME INTRO SCREEN ----------------
@Composable
fun WelcomeIntroScreen(
    username: String,
    ageGroup: AgeGroup,
    pageBgColor: Color,
    pageTextColor: Color,
    accentColor: Color,
    onContinue: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(pageBgColor, RoundedCornerShape(24.dp))
                .border(1.5.dp, BrandShadeBorder, RoundedCornerShape(24.dp))
                .padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .background(BrandShadeSoft, CircleShape)
                    .border(1.5.dp, BrandShadeMedium, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = BrandShadeBold, modifier = Modifier.size(34.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Welcome to $APP_DISPLAY_NAME, $username!",
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Serif,
                color = pageTextColor,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Tailored for ${ageGroup.title}",
                fontSize = 13.sp,
                color = BrandShadeBold,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrandShadeSoft),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Mic, contentDescription = null, tint = BrandShadeBold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Read aloud at your natural pace.", fontSize = 13.sp, color = BrandShadeDeep)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = BrandShadeBold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Words illuminate as you speak them in real time.", fontSize = 13.sp, color = BrandShadeDeep)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lightbulb, contentDescription = null, tint = BrandShadeBold, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Get instant audio pronunciation whenever you hesitate.", fontSize = 13.sp, color = BrandShadeDeep)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = onContinue,
                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                IconLabel("Let's Begin Reading", Icons.Filled.ArrowForward, iconAtEnd = true, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
