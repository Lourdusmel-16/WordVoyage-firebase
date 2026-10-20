package com.sample.readingapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp

// A Text with a Material icon next to it. Replaces the old emoji-in-text approach.
// Accepts the same common parameters as Text so it can be dropped in wherever Text was used.
@Composable
fun IconLabel(
    text: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    iconTint: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    iconAtEnd: Boolean = false
) {
    val resolvedTint = when {
        iconTint != Color.Unspecified -> iconTint
        color != Color.Unspecified -> color
        else -> LocalContentColor.current
    }
    val iconSize = if (fontSize == TextUnit.Unspecified) 18.dp else (fontSize.value * 1.2f).dp

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
    ) {
        if (icon != null && !iconAtEnd) {
            Icon(icon, contentDescription = null, tint = resolvedTint, modifier = Modifier.size(iconSize))
        }
        Text(
            text = text,
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            textAlign = textAlign,
            maxLines = maxLines,
            softWrap = softWrap,
            overflow = overflow
        )
        if (icon != null && iconAtEnd) {
            Icon(icon, contentDescription = null, tint = resolvedTint, modifier = Modifier.size(iconSize))
        }
    }
}
