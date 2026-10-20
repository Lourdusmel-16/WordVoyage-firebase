package com.sample.readingapp.theme

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// TEXT FIELD COLORS THAT STAY VISIBLE IN SEPIA / DAY / NIGHT THEMES
@Composable
fun themedFieldColors(textColor: Color) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = textColor,
    unfocusedTextColor = textColor,
    focusedLabelColor = BrandShadeMedium,
    unfocusedLabelColor = textColor.copy(alpha = 0.7f),
    cursorColor = textColor,
    focusedBorderColor = BrandShadeBold,
    unfocusedBorderColor = BrandShadeBorder
)
