package com.sample.readingapp.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.ui.graphics.vector.ImageVector

enum class ReaderTheme(val title: String, val icon: ImageVector) {
    SEPIA("Sepia", Icons.Filled.Description),
    DAY("Day", Icons.Filled.LightMode),
    NIGHT("Night", Icons.Filled.DarkMode)
}

// MATCH DEVICE: PICKS NIGHT OR DAY TO MATCH THE PHONE'S CURRENT SYSTEM DARK MODE
fun deviceTheme(context: Context): ReaderTheme {
    val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return if (nightMask == Configuration.UI_MODE_NIGHT_YES) ReaderTheme.NIGHT else ReaderTheme.DAY
}
