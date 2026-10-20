package com.sample.readingapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sample.readingapp.theme.*

@Composable
fun MatchDeviceMenuItem(onThemeChanged: (ReaderTheme) -> Unit, onDone: () -> Unit) {
    val ctx = LocalContext.current
    DropdownMenuItem(
        text = { IconLabel("Match Device", Icons.Filled.PhoneAndroid, fontWeight = FontWeight.SemiBold) },
        onClick = {
            onThemeChanged(deviceTheme(ctx))
            onDone()
        }
    )
}

@Composable
fun MatchDeviceButton(onThemeChanged: (ReaderTheme) -> Unit, textColor: Color) {
    val ctx = LocalContext.current
    OutlinedButton(
        onClick = { onThemeChanged(deviceTheme(ctx)) },
        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth().height(40.dp)
    ) {
        IconLabel("Match Device", Icons.Filled.PhoneAndroid, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = textColor)
    }
}
