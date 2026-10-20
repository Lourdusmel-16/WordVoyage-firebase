package com.sample.readingapp.ui.reader

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontFamily
import com.sample.readingapp.ui.components.WordMeaningDialog

// Everything for "long-press a word to see its meaning" lives in this file,
// so ReadingScreen.kt only needs a few small hooks.

/** Holds the word whose meaning should be shown (null = nothing shown). */
class WordMeaningController {
    var word by mutableStateOf<String?>(null)
}

/** Keeps the latest text layout of the page so a touch position can be turned into a word. */
class WordLayoutHolder {
    var layout: TextLayoutResult? = null
}

/** Removes punctuation around a word; returns null if nothing is left. */
fun wordForMeaning(raw: String?): String? {
    val shown = raw?.trim { !it.isLetterOrDigit() } ?: return null
    return shown.ifEmpty { null }
}

/** Finds which word of the page was touched. The page text is the words joined by single spaces. */
fun wordAtPosition(layout: TextLayoutResult?, words: List<String>, touch: Offset): String? {
    return try {
        if (layout == null || touch.y > layout.size.height) return null
        val charIndex = layout.getOffsetForPosition(touch)
        var start = 0
        for (w in words) {
            val end = start + w.length + 1
            if (charIndex < end) return wordForMeaning(w)
            start = end
        }
        null
    } catch (e: Exception) {
        null
    }
}

/** Single tap = onTap, long press = onLongPress (with the touch position). */
fun Modifier.tapOrLongPress(onTap: () -> Unit, onLongPress: (Offset) -> Unit): Modifier = composed {
    val currentTap by rememberUpdatedState(onTap)
    val currentLongPress by rememberUpdatedState(onLongPress)
    Modifier.pointerInput(Unit) {
        detectTapGestures(
            onTap = { currentTap() },
            onLongPress = { currentLongPress(it) }
        )
    }
}

/** Shows the meaning box when a word is set, and pauses reading while it is open. */
@Composable
fun WordMeaningHost(
    controller: WordMeaningController,
    pageBgColor: Color,
    pageTextColor: Color,
    highlightColor: Color,
    fontFamily: FontFamily,
    isPaused: Boolean,
    onPausedChange: (Boolean) -> Unit
) {
    val word = controller.word
    val latestPaused by rememberUpdatedState(isPaused)
    val latestSetPaused by rememberUpdatedState(onPausedChange)
    var resumeTo by remember { mutableStateOf(false) }

    LaunchedEffect(word != null) {
        if (word != null) {
            resumeTo = latestPaused
            latestSetPaused(true)
        }
    }

    if (word != null) {
        WordMeaningDialog(
            word = word,
            pageBgColor = pageBgColor,
            pageTextColor = pageTextColor,
            highlightColor = highlightColor,
            fontFamily = fontFamily,
            onDismiss = {
                controller.word = null
                onPausedChange(resumeTo)
            }
        )
    }
}
