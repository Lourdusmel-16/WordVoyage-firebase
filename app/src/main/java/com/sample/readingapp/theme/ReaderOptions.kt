package com.sample.readingapp.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

enum class ReadingDisplayMode(val title: String) {
    PAGE("Full Page"),
    WORD_POPUP("Word Pop-up")
}

enum class AccessibleFont(val title: String, val family: FontFamily) {
    SERIF("Serif Novel", FontFamily.Serif),
    SANS_DYSLEXIA("Dyslexia Friendly", FontFamily.SansSerif),
    MONO("Clean Mono", FontFamily.Monospace)
}

val HighlightPalettes = listOf(
    Color(0xFF86EFAC) to "Pastel Mint",
    Color(0xFF93C5FD) to "Pastel Sky",
    Color(0xFFFDBA74) to "Pastel Peach",
    Color(0xFFFDE047) to "Pastel Lemon",
    Color(0xFFF472B6) to "Pastel Rose"
)
