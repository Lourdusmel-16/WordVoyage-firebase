package com.sample.readingapp.ui.reader

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.google.firebase.auth.FirebaseAuth
import com.sample.readingapp.data.*
import com.sample.readingapp.speech.*
import com.sample.readingapp.theme.*
import com.sample.readingapp.ui.*
import com.sample.readingapp.ui.components.*
import com.sample.readingapp.util.*

// ---------------- GUIDE SESSION LOGIC ----------------
// Shows the guide once per login (keyed by uid + last sign-in time) and supports manual replay.
object GuideSession {
    private const val PREFS = "wordvoyage_guide_prefs"
    private const val KEY_LAST = "last_guided_login"

    // Registered by WordVoyageApp: jumps to a random story and opens the guide
    var onReplay: (() -> Unit)? = null
    var replayRequested = false

    private fun loginKey(): String? {
        val u = FirebaseAuth.getInstance().currentUser ?: return null
        return "${u.uid}_${u.metadata?.lastSignInTimestamp ?: 0L}"
    }

    fun consumeShouldShow(context: Context): Boolean {
        if (replayRequested) {
            replayRequested = false
            return true
        }
        val key = loginKey() ?: return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (prefs.getString(KEY_LAST, null) == key) return false
        prefs.edit().putString(KEY_LAST, key).apply()
        return true
    }
}

private const val GUIDE_STEPS = 7

private enum class GuideAnchor { CENTER, TOP, BOTTOM }

// Where each step's pop-up sits on the real reading screen
private val STEP_ANCHORS = listOf(
    GuideAnchor.CENTER,  // 0 welcome
    GuideAnchor.TOP,     // 1 Aa (change text)
    GuideAnchor.TOP,     // 2 palette (highlight color)
    GuideAnchor.TOP,     // 3 speaker (read aloud)
    GuideAnchor.BOTTOM,  // 4 word suggestion box
    GuideAnchor.CENTER,  // 5 long-press a word for its meaning
    GuideAnchor.CENTER   // 6 tap / read to move on
)

// Distance in dp from the screen's right edge to the CENTER of each top-bar icon
// (tweak these if an arrow looks slightly off on your device)
private val STEP_ICON_CENTER_FROM_RIGHT = listOf(0, 63, 173, 29, 0, 0, 0)

// Size of the pulsing ring drawn on the real icon: width, height
private val STEP_RING_WIDTH = listOf(0, 34, 62, 34, 0, 0, 0)

// Vertical offsets (dp) used to dock the pop-up under the top bar / above the suggestion box
private const val GUIDE_TOP_OFFSET_DP = 44
private const val GUIDE_BOTTOM_OFFSET_DP = 152

// ---------------- GUIDE TEXT (edit wording here) ----------------
// step 1 shows the text "Aa" instead of an icon, so its entry is null
private val GUIDE_STEP_ICONS: List<ImageVector?> = listOf(
    Icons.Filled.Star, null, Icons.Filled.Palette, Icons.Filled.VolumeUp, Icons.Filled.Lightbulb, Icons.Filled.MenuBook, Icons.Filled.TouchApp
)

private val GUIDE_TITLES = listOf(
    "Welcome to WordVoyage!",
    "Make the Text Yours",
    "Pick a Highlight Color",
    "Listen to the Story",
    "Your Word Helper",
    "Long-Press for the Meaning",
    "Keep the Story Going"
)

private val GUIDE_HINTS = listOf(
    "Let's take a 1-minute tour of the buttons on your reading page. It's quick and easy!",
    "Tap Aa at the top right to change the font, the text size and the page color. Try the size buttons below!",
    "Tap the palette icon at the top right to choose the color that lights up each word as you read. Tap a color to try it!",
    "Tap the speaker icon at the top right and the app will read the page to you. Tap the stop icon whenever you want it to stop.",
    "This box shows the word you're on and how to say it. Tap Listen to hear it slowly, or Help for extra support.",
    "Don't know what a word means? Press and hold that word on the page for a moment and a box will pop up with its meaning. Try it on the word below!",
    "To move to the next word, tap the page or read the word out loud. Give the sentence below a tap!"
)

private val GUIDE_WELCOME_POINTS = listOf(
    Icons.Filled.AutoAwesome to "Change the font, size and page color",
    Icons.Filled.Palette to "Choose your own highlight color",
    Icons.Filled.VolumeUp to "Listen while the story is read to you",
    Icons.Filled.Lightbulb to "Get help with hard words"
)

private const val GUIDE_STEP1_SAMPLE = "Reading is an adventure!"
private const val GUIDE_STEP2_BEFORE = "Each word you read "
private const val GUIDE_STEP2_GLOW = "glows"
private const val GUIDE_STEP2_AFTER = " just like this!"
private const val GUIDE_STEP3_BUTTON = "Tap to hear a sample"
private const val GUIDE_STEP3_SPEECH = "Hello! Let's read this story together."
private const val GUIDE_STEP3_NOTE = "It starts from the word you're on, so you can begin anywhere."
private const val GUIDE_STEP4_NOTE = "Stuck on a word for 6 seconds? Your helper will pop up by itself."
private const val GUIDE_STEP_MEANING_WORD = "adventure"
private const val GUIDE_STEP_MEANING_PROMPT = "Press and hold the word"
private const val GUIDE_STEP_MEANING_TEXT = "an exciting or unusual thing that you do or that happens to you."
private const val GUIDE_STEP_MEANING_NOTE = "Needs internet. Reading pauses while the meaning box is open."
private const val GUIDE_STEP5_DONE = "Great job! Tap again to try once more."
private const val GUIDE_STEP5_PROMPT = "Tap the sentence or read it out loud"
private const val GUIDE_FINISH_BUTTON = "Let's Read!"

// ---------------- ARROW ----------------
@Composable
private fun GuideArrow(pointUp: Boolean, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(20.dp, 12.dp)) {
        val p = Path().apply {
            if (pointUp) {
                moveTo(size.width / 2f, 0f)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
            } else {
                moveTo(0f, 0f)
                lineTo(size.width, 0f)
                lineTo(size.width / 2f, size.height)
            }
            close()
        }
        drawPath(p, color)
    }
}

// ---------------- INTERACTIVE GUIDE DIALOG ----------------
@Composable
fun ReadingGuideDialog(
    highlightColor: Color,
    pageBgColor: Color,
    pageTextColor: Color,
    fontFamily: FontFamily,
    fontSizeSp: Int,
    onHighlightColorChanged: (Color) -> Unit,
    onFontSizeChanged: (Int) -> Unit,
    onFinish: () -> Unit
) {
    var step by remember { mutableStateOf(0) }
    var direction by remember { mutableStateOf(1) }
    val tts = SpeechEngineCache.cachedTts

    val demoWords = remember { listOf("The", "little", "fox", "jumped", "over", "the", "log.") }
    var demoIdx by remember { mutableStateOf(0) }
    var meaningShown by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { tts?.stop() }
    }

    val pulse by rememberInfiniteTransition(label = "guidePulse").animateFloat(
        initialValue = 1f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "guidePulseValue"
    )

    val anchor = STEP_ANCHORS[step]
    // inside the 14dp side padding, arrow is 20dp wide
    val arrowEndTarget = (STEP_ICON_CENTER_FROM_RIGHT[step] - 14 - 10).coerceAtLeast(0)
    val arrowEnd by animateDpAsState(arrowEndTarget.dp, label = "arrowEnd")

    fun goTo(target: Int) {
        val t = target.coerceIn(0, GUIDE_STEPS - 1)
        direction = if (t >= step) 1 else -1
        step = t
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        // lighter dim so the real icons stay visible behind the guide
        val dialogView = LocalView.current
        SideEffect {
            (dialogView.parent as? DialogWindowProvider)?.window?.setDimAmount(0.4f)
        }

        Box(modifier = Modifier.fillMaxSize()) {

            // PULSING RING ON THE REAL ICON
            if (step in 1..3) {
                val ringW = STEP_RING_WIDTH[step]
                val ringEnd = (STEP_ICON_CENTER_FROM_RIGHT[step] - ringW / 2).coerceAtLeast(0)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = ringEnd.dp)
                        .size(ringW.dp, 34.dp)
                        .graphicsLayer { scaleX = pulse; scaleY = pulse }
                        .border(
                            2.5.dp,
                            highlightColor,
                            if (step == 2) RoundedCornerShape(10.dp) else CircleShape
                        )
                )
            }

            Column(
                modifier = Modifier
                    .align(
                        when (anchor) {
                            GuideAnchor.TOP -> Alignment.TopCenter
                            GuideAnchor.BOTTOM -> Alignment.BottomCenter
                            GuideAnchor.CENTER -> Alignment.Center
                        }
                    )
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
                    .padding(
                        top = if (anchor == GuideAnchor.TOP) GUIDE_TOP_OFFSET_DP.dp else 0.dp,
                        bottom = if (anchor == GuideAnchor.BOTTOM) GUIDE_BOTTOM_OFFSET_DP.dp else 0.dp
                    )
            ) {
                // ARROW (above card for TOP, below card for BOTTOM)
                if (anchor == GuideAnchor.TOP) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        GuideArrow(
                            pointUp = true,
                            color = highlightColor,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = arrowEnd)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 470.dp)
                        .background(pageBgColor, RoundedCornerShape(20.dp))
                        .border(2.dp, highlightColor, RoundedCornerShape(20.dp))
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // HEADER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .background(BrandShadeSoft, RoundedCornerShape(12.dp))
                                .border(1.dp, BrandShadeBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            IconLabel(
                                "Quick Tour • Step ${step + 1} of $GUIDE_STEPS",
                                Icons.Filled.Explore,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandShadeDeep
                            )
                        }
                        TextButton(onClick = onFinish) {
                            Text("Skip tour", color = Color.Gray, fontSize = 12.sp)
                        }
                    }

                    // PROGRESS DOTS
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(GUIDE_STEPS) { i ->
                            val w by animateDpAsState(if (i == step) 22.dp else 8.dp, label = "dotW")
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(w)
                                    .background(
                                        if (i <= step) highlightColor else Color.Gray.copy(alpha = 0.25f),
                                        RoundedCornerShape(4.dp)
                                    )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // STEP BODY
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            (fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it / 4 * direction })
                                .togetherWith(fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { -it / 4 * direction })
                        },
                        label = "guideStep"
                    ) { s ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val stepIcon = GUIDE_STEP_ICONS[s]
                            val title = GUIDE_TITLES[s]
                            val hint = GUIDE_HINTS[s]

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .graphicsLayer { scaleX = pulse * 0.95f; scaleY = pulse * 0.95f }
                                        .background(highlightColor.copy(alpha = 0.3f), CircleShape)
                                        .border(2.dp, highlightColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (stepIcon == null) {
                                        Text(
                                            "Aa",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = pageTextColor
                                        )
                                    } else {
                                        Icon(
                                            stepIcon,
                                            contentDescription = null,
                                            tint = pageTextColor,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = fontFamily,
                                    color = pageTextColor
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text(hint, fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(12.dp))

                            when (s) {
                                0 -> {
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = BrandShadeSoft),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            GUIDE_WELCOME_POINTS.forEach { (pointIcon, pointText) ->
                                                IconLabel(pointText, pointIcon, fontSize = 13.sp, color = BrandShadeDeep)
                                            }
                                        }
                                    }
                                }

                                1 -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        OutlinedButton(
                                            onClick = { if (fontSizeSp > 15) onFontSizeChanged(fontSizeSp - 2) },
                                            shape = CircleShape,
                                            modifier = Modifier.size(40.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) { Text("A-", fontWeight = FontWeight.Bold, color = pageTextColor) }
                                        Text(
                                            "$fontSizeSp pt",
                                            fontWeight = FontWeight.Bold,
                                            color = pageTextColor,
                                            modifier = Modifier.padding(horizontal = 18.dp)
                                        )
                                        OutlinedButton(
                                            onClick = { if (fontSizeSp < 28) onFontSizeChanged(fontSizeSp + 2) },
                                            shape = CircleShape,
                                            modifier = Modifier.size(40.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) { Text("A+", fontWeight = FontWeight.Bold, color = pageTextColor) }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(highlightColor.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                                            .padding(12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            GUIDE_STEP1_SAMPLE,
                                            fontSize = fontSizeSp.coerceAtMost(26).sp,
                                            fontFamily = fontFamily,
                                            color = pageTextColor,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }

                                2 -> {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        HighlightPalettes.forEach { (color, _) ->
                                            val isSelected = color == highlightColor
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .background(color, CircleShape)
                                                    .border(
                                                        if (isSelected) 3.dp else 1.dp,
                                                        if (isSelected) pageTextColor else Color.Transparent,
                                                        CircleShape
                                                    )
                                                    .clickable { onHighlightColorChanged(color) }
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = buildAnnotatedString {
                                            withStyle(SpanStyle(background = highlightColor.copy(alpha = 0.35f), color = pageTextColor)) {
                                                append(GUIDE_STEP2_BEFORE)
                                            }
                                            withStyle(SpanStyle(background = highlightColor.copy(alpha = 0.8f), color = pageTextColor, textDecoration = TextDecoration.Underline)) {
                                                append(GUIDE_STEP2_GLOW)
                                            }
                                            withStyle(SpanStyle(color = Color.Gray)) { append(GUIDE_STEP2_AFTER) }
                                        },
                                        fontSize = 17.sp,
                                        fontFamily = fontFamily,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                3 -> {
                                    Button(
                                        onClick = {
                                            tts?.speak(GUIDE_STEP3_SPEECH, TextToSpeech.QUEUE_FLUSH, null, "guide_demo")
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().height(46.dp)
                                    ) {
                                        IconLabel(GUIDE_STEP3_BUTTON, Icons.Filled.VolumeUp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        GUIDE_STEP3_NOTE,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                4 -> {
                                    val sampleWord = "adventure"
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = pageBgColor),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.5.dp, highlightColor.copy(alpha = 0.6f))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 14.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(sampleWord, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = fontFamily, color = pageTextColor)
                                                Text("[${getPhoneticHint(sampleWord)}]", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = highlightColor)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                                FilledTonalButton(
                                                    onClick = {
                                                        tts?.setSpeechRate(0.65f)
                                                        tts?.speak(cleanWord(sampleWord), TextToSpeech.QUEUE_FLUSH, null, "guide_word")
                                                        tts?.setSpeechRate(0.75f)
                                                    },
                                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = highlightColor.copy(alpha = 0.25f), contentColor = pageTextColor),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) { IconLabel("Listen", Icons.Filled.VolumeUp, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                                                OutlinedButton(
                                                    onClick = {},
                                                    border = BorderStroke(1.dp, highlightColor),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) { IconLabel("Help", Icons.Filled.Lightbulb, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pageTextColor) }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        GUIDE_STEP4_NOTE,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                5 -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(highlightColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .border(1.dp, highlightColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .tapOrLongPress(
                                                onTap = {},
                                                onLongPress = { meaningShown = true }
                                            )
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            GUIDE_STEP_MEANING_WORD,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = fontFamily,
                                            color = pageTextColor,
                                            textDecoration = TextDecoration.Underline
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (meaningShown) {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = pageBgColor),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.5.dp, highlightColor.copy(alpha = 0.6f))
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                IconLabel(
                                                    GUIDE_STEP_MEANING_WORD,
                                                    Icons.Filled.MenuBook,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = fontFamily,
                                                    color = pageTextColor
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(GUIDE_STEP_MEANING_TEXT, fontSize = 13.sp, color = pageTextColor)
                                            }
                                        }
                                    } else {
                                        IconLabel(
                                            text = GUIDE_STEP_MEANING_PROMPT,
                                            icon = Icons.Filled.TouchApp,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BrandShadeBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        GUIDE_STEP_MEANING_NOTE,
                                        fontSize = 11.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                else -> {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(highlightColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                            .border(1.dp, highlightColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                            .clickable { demoIdx = if (demoIdx >= demoWords.size) 0 else demoIdx + 1 }
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = buildAnnotatedString {
                                                demoWords.forEachIndexed { i, w ->
                                                    when {
                                                        i < demoIdx -> withStyle(SpanStyle(color = pageTextColor, background = highlightColor.copy(alpha = 0.35f))) { append("$w ") }
                                                        i == demoIdx -> withStyle(SpanStyle(color = pageTextColor, background = highlightColor.copy(alpha = 0.85f), textDecoration = TextDecoration.Underline)) { append("$w ") }
                                                        else -> withStyle(SpanStyle(color = Color.Gray)) { append("$w ") }
                                                    }
                                                }
                                            },
                                            fontSize = 20.sp,
                                            fontFamily = fontFamily,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    IconLabel(
                                        text = if (demoIdx >= demoWords.size) GUIDE_STEP5_DONE else GUIDE_STEP5_PROMPT,
                                        icon = if (demoIdx >= demoWords.size) Icons.Filled.EmojiEvents else Icons.Filled.TouchApp,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandShadeBold,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // NAVIGATION
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { goTo(step - 1) },
                            enabled = step > 0,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) { IconLabel("Back", Icons.Filled.ArrowBack, color = pageTextColor, fontSize = 13.sp) }

                        Button(
                            onClick = { if (step < GUIDE_STEPS - 1) goTo(step + 1) else onFinish() },
                            colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1.4f).height(44.dp)
                        ) {
                            IconLabel(
                                if (step < GUIDE_STEPS - 1) "Next" else GUIDE_FINISH_BUTTON,
                                if (step < GUIDE_STEPS - 1) Icons.Filled.ArrowForward else Icons.Filled.MenuBook,
                                iconAtEnd = true,
                                color = Color(0xFF1E293B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                        }
                    }
                }

                if (anchor == GuideAnchor.BOTTOM) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        GuideArrow(
                            pointUp = false,
                            color = highlightColor,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}
