package com.sample.readingapp.ui.reader

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.sample.readingapp.data.*
import com.sample.readingapp.speech.*
import com.sample.readingapp.theme.*
import com.sample.readingapp.ui.components.*
import com.sample.readingapp.util.*
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Seconds of silence on a word before the "Need a hand?" help popup appears. */
private const val HELP_PROMPT_SECONDS = 6

// ---------------- READING SCREEN ----------------
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReadingScreen(
    story: Story,
    initialScreenIndex: Int = 0,
    initialWordIndex: Int = 0,
    savedPageWords: Map<Int, Int> = emptyMap(),
    username: String,
    highlightColor: Color,
    onHighlightColorChanged: (Color) -> Unit = {},
    readerTheme: ReaderTheme,
    pageBgColor: Color,
    pageTextColor: Color,
    fontSizeSp: Int,
    accessibleFont: AccessibleFont,
    displayMode: ReadingDisplayMode,
    onDisplayModeChanged: (ReadingDisplayMode) -> Unit,
    onThemeChanged: (ReaderTheme) -> Unit,
    onFontSizeChanged: (Int) -> Unit,
    onFontFamilyChanged: (AccessibleFont) -> Unit,
    onProgressChanged: (pageIndex: Int, wordIndex: Int) -> Unit = { _, _ -> },
    onExitToHome: () -> Unit,
    onStoryCompleted: (Int, Int, Int, List<String>) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var countdownNumber by remember { mutableStateOf(3) }
    var isCountdownActive by remember { mutableStateOf(true) }

    val lastIndex = (story.screens.size - 1).coerceAtLeast(0)
    val totalPages = story.screens.size.coerceAtLeast(1)

    var screenIndex by remember(story.id) { mutableStateOf(initialScreenIndex.coerceIn(0, lastIndex)) }

    val currentScreenText = remember(screenIndex, story) {
        if (story.screens.isNotEmpty() && screenIndex < story.screens.size) story.screens[screenIndex] else ""
    }
    val words = remember(currentScreenText) {
        currentScreenText.split("\\s+".toRegex()).filter { it.isNotBlank() }
    }

    // START FROM THE LAST REMEMBERED WORD (IF IT WAS THE LAST WORD, START FROM THE LAST WORD)
    var currentWordIndex by remember(story.id) {
        mutableStateOf(if (words.isNotEmpty()) initialWordIndex.coerceIn(0, (words.size - 1).coerceAtLeast(0)) else 0)
    }

    var showRereadDialog by remember { mutableStateOf(false) }
    var showExitStoryConfirmDialog by remember { mutableStateOf(false) }

    BackHandler {
        showExitStoryConfirmDialog = true
    }

    var isPaused by remember { mutableStateOf(false) }
    var isTtsSpeaking by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    var wordsHighlightedCount by remember { mutableStateOf(0) }
    var hesitationsCount by remember { mutableStateOf(0) }
    val trickyWords = remember { mutableStateListOf<String>() }

    var lastHeardPhrase by remember { mutableStateOf("") }
    var lastSpokenTimestamp by remember { mutableStateOf(System.currentTimeMillis()) }

    var showHelpPrompt by remember { mutableStateOf(false) }
    var showStuckWordDialog by remember { mutableStateOf(false) }
    val meaningController = remember { WordMeaningController() }
    val layoutHolder = remember { WordLayoutHolder() }
    var showReaderSettingsSheet by remember { mutableStateOf(false) }
    // LAST WORD PER PAGE (PRE-FILLED WITH WHAT WAS SAVED IN FIREBASE)
    val screenProgress = remember(story.id) {
        mutableStateMapOf<Int, Int>().apply { putAll(savedPageWords) }
    }
    var showBackChoiceDialog by remember { mutableStateOf(false) }

    val currentWordsState by rememberUpdatedState(words)
    val currentTargetIndexState by rememberUpdatedState(currentWordIndex)

    val unreadTextColor = if (readerTheme == ReaderTheme.NIGHT) Color(0xFF64748B) else Color(0xFF78716C)
    var dragOffsetX by remember { mutableStateOf(0f) }

    val bookScrollState = rememberScrollState()
    val liveWpm = if (elapsedSeconds > 2) ((wordsHighlightedCount * 60) / elapsedSeconds).coerceIn(0, 240) else 0

    val infiniteGlowTransition = rememberInfiniteTransition(label = "wordGlowPulse")
    val glowIntensity by infiniteGlowTransition.animateFloat(
        initialValue = 0.50f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "currentWordGlow"
    )

    LaunchedEffect(screenIndex, currentWordIndex) {
        // IF THE PAGE IS FINISHED, REMEMBER THE LAST WORD OF THE PAGE (NOT AN OUT-OF-RANGE INDEX)
        val wordToRemember = if (words.isNotEmpty()) currentWordIndex.coerceAtMost(words.size - 1) else currentWordIndex
        onProgressChanged(screenIndex, wordToRemember)
        if (currentWordIndex < words.size) {
            screenProgress[screenIndex] = currentWordIndex
        }
    }

    LaunchedEffect(currentWordIndex) {
        if (words.isNotEmpty() && displayMode == ReadingDisplayMode.PAGE) {
            val progress = currentWordIndex.toFloat() / words.size
            val targetScroll = (bookScrollState.maxValue * progress).toInt()

            bookScrollState.scrollTo(targetScroll)
        }
    }

    LaunchedEffect(
        currentWordIndex,
        screenIndex,
        isPaused,
        isTtsSpeaking,
        showReaderSettingsSheet,
        showBackChoiceDialog,
        showStuckWordDialog,
        showHelpPrompt,
        showRereadDialog,
        showExitStoryConfirmDialog,
        isCountdownActive
    ) {
        if (isCountdownActive) return@LaunchedEffect

        var silentSeconds = 0
        lastSpokenTimestamp = System.currentTimeMillis()

        while (!isPaused && !isTtsSpeaking && !showReaderSettingsSheet && !showBackChoiceDialog &&
            !showStuckWordDialog && !showHelpPrompt && !showRereadDialog && !showExitStoryConfirmDialog &&
            currentWordIndex < words.size
        ) {
            delay(1000)
            if (System.currentTimeMillis() - lastSpokenTimestamp < 3500L) {
                silentSeconds = 0
            } else {
                silentSeconds++
            }

            if (silentSeconds >= HELP_PROMPT_SECONDS) {
                showHelpPrompt = true
                hesitationsCount++
                if (!trickyWords.contains(words[currentWordIndex])) {
                    trickyWords.add(words[currentWordIndex])
                }
                break
            }
        }
    }

    LaunchedEffect(isPaused, isTtsSpeaking, showBackChoiceDialog, showStuckWordDialog, showHelpPrompt, showReaderSettingsSheet, showRereadDialog, showExitStoryConfirmDialog, isCountdownActive) {
        if (isCountdownActive) return@LaunchedEffect
        while (!isPaused && !isTtsSpeaking && !showBackChoiceDialog && !showStuckWordDialog && !showHelpPrompt && !showReaderSettingsSheet && !showRereadDialog && !showExitStoryConfirmDialog) {
            delay(1000)
            elapsedSeconds++
        }
    }

    val ttsInstance = SpeechEngineCache.cachedTts
    DisposableEffect(Unit) {
        ttsInstance?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) { isTtsSpeaking = true }
            override fun onDone(utteranceId: String?) { isTtsSpeaking = false }
            override fun onError(utteranceId: String?) { isTtsSpeaking = false }
        })
        onDispose {
            ttsInstance?.stop()
        }
    }

    var hasMicPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted -> hasMicPermission = isGranted }

    LaunchedEffect(Unit) {
        if (!hasMicPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val isAnyPopupActive by rememberUpdatedState(
        showBackChoiceDialog || showStuckWordDialog || showHelpPrompt || showReaderSettingsSheet || showRereadDialog || showExitStoryConfirmDialog
    )

    var speechManager by remember { mutableStateOf<SherpaSpeechManager?>(null) }
    var lastTokenProcessedTime by remember { mutableStateOf(0L) }

    LaunchedEffect(story.id) {
        isCountdownActive = true

        val initSherpaJob = launch(Dispatchers.IO) {
            val sm = SpeechEngineCache.getOrCreateSherpa(context) { candidates ->
                if (isPaused || isTtsSpeaking || isAnyPopupActive) return@getOrCreateSherpa

                val currentTime = System.currentTimeMillis()
                if (currentTime - lastTokenProcessedTime < 80L) return@getOrCreateSherpa

                val activeWords = currentWordsState
                var scanIdx = currentTargetIndexState

                for (candidate in candidates) {
                    val rawSpoken = candidate.lowercase().replace("[^a-z0-9 ]".toRegex(), "").trim()
                    if (rawSpoken.isBlank()) continue

                    val spokenTokens = rawSpoken.split("\\s+".toRegex()).filter { it.isNotBlank() }
                    val recent3Tokens = spokenTokens.takeLast(3)
                    if (recent3Tokens.isEmpty()) continue

                    lastHeardPhrase = recent3Tokens.joinToString(" ")
                    lastSpokenTimestamp = currentTime
                    lastTokenProcessedTime = currentTime

                    if (!isCountdownActive) {
                        var matchedCount = 0
                        for (token in recent3Tokens) {
                            if (scanIdx < activeWords.size && isSimilarWord(token, activeWords[scanIdx])) {
                                scanIdx++
                                matchedCount++
                            }
                        }

                        if (matchedCount > 0) {
                            showHelpPrompt = false
                            wordsHighlightedCount += matchedCount
                            currentWordIndex = scanIdx
                            break
                        }
                    }
                }
            }

            if (hasMicPermission) {
                sm.startListening()
            }

            withContext(Dispatchers.Main) {
                speechManager = sm
            }
        }

        countdownNumber = 3
        delay(1000)
        countdownNumber = 2
        delay(1000)
        countdownNumber = 1
        delay(1000)

        initSherpaJob.join()

        isCountdownActive = false
        lastHeardPhrase = ""
        lastSpokenTimestamp = System.currentTimeMillis()
    }

    val shouldRunMic = hasMicPermission && !isPaused && !isTtsSpeaking

    LaunchedEffect(shouldRunMic, speechManager) {
        if (shouldRunMic && speechManager != null) {
            coroutineScope.launch(Dispatchers.IO) {
                speechManager?.startListening()
            }
        } else {
            speechManager?.stopListening()
        }
    }

    LaunchedEffect(screenIndex) {
        bookScrollState.scrollTo(0)
        lastHeardPhrase = ""
        showHelpPrompt = false
        lastSpokenTimestamp = System.currentTimeMillis()
        if (isTtsSpeaking) {
            ttsInstance?.stop()
            isTtsSpeaking = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { speechManager?.stopListening() }
    }

    LaunchedEffect(currentWordIndex, words.size) {
        if (words.isNotEmpty() && currentWordIndex >= words.size) {
            if (screenIndex < lastIndex) {
                val nextScreen = screenIndex + 1
                currentWordIndex = screenProgress[nextScreen] ?: 0
                showHelpPrompt = false
                screenIndex = nextScreen
            } else {
                onStoryCompleted(elapsedSeconds, wordsHighlightedCount, hesitationsCount, trickyWords.toList())
            }
        }
    }

    if (showExitStoryConfirmDialog) {
        Dialog(onDismissRequest = { showExitStoryConfirmDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBgColor, RoundedCornerShape(18.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Exit Story?", fontSize = 19.sp, fontWeight = FontWeight.Bold, fontFamily = accessibleFont.family, color = pageTextColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Are you sure you want to exit? Only stories where all $totalPages pages are finished will be added to your completed list.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showExitStoryConfirmDialog = false },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(1.15f).defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text(
                                text = "Continue Reading",
                                color = pageTextColor,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }
                        Button(
                            onClick = {
                                showExitStoryConfirmDialog = false
                                onExitToHome()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            modifier = Modifier.weight(0.85f).defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text(
                                text = "Yes, Exit",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                softWrap = false,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (isCountdownActive) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(pageBgColor),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(112.dp)
                        .background(highlightColor.copy(alpha = 0.25f), CircleShape)
                        .border(3.dp, highlightColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$countdownNumber",
                        fontSize = 54.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = accessibleFont.family,
                        color = pageTextColor
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = story.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = accessibleFont.family,
                    color = pageTextColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                if (initialScreenIndex > 0 || initialWordIndex > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Resuming from Page ${initialScreenIndex + 1}, Word ${initialWordIndex + 1}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = highlightColor,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Get ready, $username! Read aloud on Go",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    if (showRereadDialog) {
        Dialog(onDismissRequest = { showRereadDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBgColor, RoundedCornerShape(20.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    IconLabel("Re-read Options", Icons.Filled.Refresh, fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = accessibleFont.family, color = pageTextColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Choose where you would like to resume reading:", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            currentWordIndex = 0
                            screenProgress[screenIndex] = 0
                            lastSpokenTimestamp = System.currentTimeMillis()
                            showRereadDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 42.dp)
                    ) {
                        Text("Beginning of Page (Word 1)", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            currentWordIndex = max(0, currentWordIndex - 5)
                            screenProgress[screenIndex] = currentWordIndex
                            lastSpokenTimestamp = System.currentTimeMillis()
                            showRereadDialog = false
                        },
                        border = BorderStroke(1.dp, highlightColor),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 42.dp)
                    ) {
                        Text("Rewind 5 Words", color = pageTextColor, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val pastWords = words.take(currentWordIndex)
                    if (pastWords.isNotEmpty()) {
                        Text(
                            text = "All Previously Read Words (${pastWords.size}):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandShadeBold
                        )
                        Text(
                            text = "Tap any word below to jump directly to it:",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            pastWords.forEachIndexed { wordIndex, wordText ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        currentWordIndex = wordIndex
                                        screenProgress[screenIndex] = wordIndex
                                        lastSpokenTimestamp = System.currentTimeMillis()
                                        showRereadDialog = false
                                    },
                                    label = { Text(wordText, fontSize = 12.sp, color = pageTextColor) },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    TextButton(onClick = { showRereadDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }

    if (showHelpPrompt && currentWordIndex < words.size) {
        val stuckWord = words[currentWordIndex]

        Dialog(onDismissRequest = { showHelpPrompt = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBgColor, RoundedCornerShape(18.dp))
                    .border(1.5.dp, highlightColor, RoundedCornerShape(18.dp))
                    .padding(22.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconLabel("Need a hand?", Icons.Filled.Psychology, fontSize = 20.sp, fontWeight = FontWeight.Bold, fontFamily = accessibleFont.family, color = pageTextColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Need help pronouncing \"$stuckWord\"?", fontSize = 14.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            showHelpPrompt = false
                            showStuckWordDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        IconLabel("Yes, Help Me", Icons.Filled.Lightbulb, iconAtEnd = true, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            showHelpPrompt = false
                            lastSpokenTimestamp = System.currentTimeMillis()
                        },
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        IconLabel("No, I'm Trying", Icons.Filled.FitnessCenter, iconAtEnd = true, color = pageTextColor, fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showStuckWordDialog && currentWordIndex < words.size) {
        val stuckWord = words[currentWordIndex]

        Dialog(onDismissRequest = { showStuckWordDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBgColor, RoundedCornerShape(20.dp))
                    .border(1.dp, highlightColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Pronunciation Helper", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = accessibleFont.family, color = pageTextColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Listen closely and repeat:", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(text = stuckWord, fontSize = 26.sp, fontWeight = FontWeight.Bold, fontFamily = accessibleFont.family, color = highlightColor)
                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            if (isTtsSpeaking) {
                                ttsInstance?.stop()
                                isTtsSpeaking = false
                            } else {
                                ttsInstance?.setSpeechRate(0.65f)
                                ttsInstance?.speak(cleanWord(stuckWord), TextToSpeech.QUEUE_FLUSH, null, "stuck_hint")
                                ttsInstance?.setSpeechRate(0.75f)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        IconLabel(if (isTtsSpeaking) "Stop" else "Hear Word (0.6x)", if (isTtsSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp, color = pageTextColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            showStuckWordDialog = false
                            if (currentWordIndex < words.size) currentWordIndex++
                            lastSpokenTimestamp = System.currentTimeMillis()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        IconLabel("Skip Word", Icons.Filled.SkipNext, iconAtEnd = true, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = {
                        showStuckWordDialog = false
                        lastSpokenTimestamp = System.currentTimeMillis()
                    }) {
                        IconLabel("I'll Try Reading It", Icons.Filled.Mic, iconAtEnd = true, color = pageTextColor, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    WordMeaningHost(
        controller = meaningController,
        pageBgColor = pageBgColor,
        pageTextColor = pageTextColor,
        highlightColor = highlightColor,
        fontFamily = accessibleFont.family,
        isPaused = isPaused,
        onPausedChange = { isPaused = it }
    )

    if (showReaderSettingsSheet) {
        Dialog(onDismissRequest = { showReaderSettingsSheet = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBgColor, RoundedCornerShape(18.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Reader Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = accessibleFont.family, color = pageTextColor)
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("HIGHLIGHT COLOR", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HighlightPalettes.forEach { (color, _) ->
                            val isSelected = color == highlightColor
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) pageTextColor else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onHighlightColorChanged(color) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("PAGE THEME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ReaderTheme.values().forEach { theme ->
                            FilterChip(
                                selected = theme == readerTheme,
                                onClick = { onThemeChanged(theme) },
                                label = { IconLabel(theme.title, theme.icon, color = pageTextColor) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    MatchDeviceButton(onThemeChanged = onThemeChanged, textColor = pageTextColor)

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("TYPOGRAPHY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        AccessibleFont.values().forEach { font ->
                            FilterChip(
                                selected = font == accessibleFont,
                                onClick = { onFontFamilyChanged(font) },
                                label = { Text(font.title, fontSize = 11.sp, color = pageTextColor) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text("FONT SCALE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { if (fontSizeSp > 15) onFontSizeChanged(fontSizeSp - 2) },
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("-", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = pageTextColor)
                        }

                        Text(
                            text = "$fontSizeSp pt",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = pageTextColor,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )

                        OutlinedButton(
                            onClick = { if (fontSizeSp < 28) onFontSizeChanged(fontSizeSp + 2) },
                            shape = CircleShape,
                            modifier = Modifier.size(42.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = pageTextColor)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { showReaderSettingsSheet = false },
                        colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Apply & Close", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showBackChoiceDialog) {
        val prevIndex = screenIndex - 1
        val prevSavedWord = screenProgress[prevIndex] ?: 0

        Dialog(onDismissRequest = { showBackChoiceDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBgColor, RoundedCornerShape(20.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Turn Back Page?", fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = accessibleFont.family, color = pageTextColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Resume where you left off or restart this page?", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            showBackChoiceDialog = false
                            screenIndex = prevIndex
                            currentWordIndex = prevSavedWord
                            showHelpPrompt = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Resume Where I Left Off", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            showBackChoiceDialog = false
                            screenIndex = prevIndex
                            currentWordIndex = 0
                            screenProgress[prevIndex] = 0
                            showHelpPrompt = false
                        },
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Restart Page from Word 1", color = Color(0xFFDC2626), fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(onClick = { showBackChoiceDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }

    var showGuide by remember(story.id) { mutableStateOf(GuideSession.consumeShouldShow(context)) }
    // Keeps the "Need a hand?" timer quiet while the guide is open; does not touch mic or pause state
    LaunchedEffect(showGuide) {
        while (showGuide) {
            lastSpokenTimestamp = System.currentTimeMillis()
            delay(1000)
        }
    }
    if (showGuide) {
        ReadingGuideDialog(
            highlightColor = highlightColor,
            pageBgColor = pageBgColor,
            pageTextColor = pageTextColor,
            fontFamily = accessibleFont.family,
            fontSizeSp = fontSizeSp,
            onHighlightColorChanged = onHighlightColorChanged,
            onFontSizeChanged = onFontSizeChanged,
            onFinish = { showGuide = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showExitStoryConfirmDialog = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    IconLabel("Exit", Icons.Filled.Close, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = story.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = accessibleFont.family,
                        color = pageTextColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = story.gradeLevel,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var showColorMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { showColorMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, highlightColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(highlightColor, CircleShape)
                                    .border(1.dp, pageTextColor.copy(alpha = 0.4f), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Filled.Palette, contentDescription = null, tint = pageTextColor, modifier = Modifier.size(14.dp))
                        }
                        DropdownMenu(
                            expanded = showColorMenu,
                            onDismissRequest = { showColorMenu = false }
                        ) {
                            HighlightPalettes.forEach { (color, name) ->
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(16.dp)
                                                    .background(color, CircleShape)
                                                    .border(
                                                        1.5.dp,
                                                        if (color == highlightColor) pageTextColor else Color.Transparent,
                                                        CircleShape
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = name,
                                                fontSize = 12.sp,
                                                fontWeight = if (color == highlightColor) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    },
                                    onClick = {
                                        onHighlightColorChanged(color)
                                        showColorMenu = false
                                    }
                                )
                            }
                        }
                    }

                    var showReadingThemeMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { showReadingThemeMenu = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(30.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp),
                            border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.35f))
                        ) {
                            Icon(readerTheme.icon, contentDescription = null, tint = pageTextColor, modifier = Modifier.size(16.dp))
                        }
                        DropdownMenu(
                            expanded = showReadingThemeMenu,
                            onDismissRequest = { showReadingThemeMenu = false }
                        ) {
                            ReaderTheme.values().forEach { theme ->
                                DropdownMenuItem(
                                    text = { IconLabel(theme.title, theme.icon) },
                                    onClick = {
                                        onThemeChanged(theme)
                                        showReadingThemeMenu = false
                                    }
                                )
                            }
                            MatchDeviceMenuItem(onThemeChanged = onThemeChanged, onDone = { showReadingThemeMenu = false })
                        }
                    }

                    OutlinedButton(
                        onClick = { showReaderSettingsSheet = true },
                        shape = CircleShape,
                        modifier = Modifier.size(30.dp),
                        contentPadding = PaddingValues(0.dp),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f))
                    ) {
                        Text("Aa", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = pageTextColor)
                    }

                    IconButton(
                        onClick = {
                            if (isTtsSpeaking) {
                                ttsInstance?.stop()
                                isTtsSpeaking = false
                            } else {
                                val remainingWords = words.drop(currentWordIndex).joinToString(" ")
                                val textToRead = if (remainingWords.isNotBlank()) remainingWords else currentScreenText
                                ttsInstance?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "tts_utterance")
                            }
                        },
                        modifier = Modifier
                            .background(if (isTtsSpeaking) Color(0xFF0284C7).copy(alpha = 0.25f) else Color.Gray.copy(alpha = 0.15f), CircleShape)
                            .size(30.dp)
                    ) {
                        Icon(if (isTtsSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp, contentDescription = null, tint = pageTextColor, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            LinearProgressIndicator(
                progress = (screenIndex + 1) / totalPages.toFloat(),
                modifier = Modifier.fillMaxWidth().height(5.dp),
                color = highlightColor,
                trackColor = Color.Gray.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconLabel("$liveWpm WPM", Icons.Filled.FlashOn, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = highlightColor)
                Text("$wordsHighlightedCount words read", fontSize = 12.sp, color = Color.Gray)
                Text("Page ${screenIndex + 1} of $totalPages", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pageTextColor)
            }

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedContent(
                targetState = screenIndex,
                transitionSpec = {
                    if (targetState > initialState) {
                        (fadeIn(animationSpec = tween(280)) + slideInHorizontally(
                            animationSpec = tween(380, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> fullWidth }
                        ) + scaleIn(
                            initialScale = 0.94f,
                            transformOrigin = TransformOrigin(0f, 0.5f),
                            animationSpec = tween(380, easing = FastOutSlowInEasing)
                        )).togetherWith(
                            fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                                animationSpec = tween(380, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> -fullWidth / 2 }
                            ) + scaleOut(
                                targetScale = 0.90f,
                                transformOrigin = TransformOrigin(0f, 0.5f),
                                animationSpec = tween(380, easing = FastOutSlowInEasing)
                            )
                        )
                    } else {
                        (fadeIn(animationSpec = tween(280)) + slideInHorizontally(
                            animationSpec = tween(380, easing = FastOutSlowInEasing),
                            initialOffsetX = { fullWidth -> -fullWidth }
                        ) + scaleIn(
                            initialScale = 0.94f,
                            transformOrigin = TransformOrigin(1f, 0.5f),
                            animationSpec = tween(380, easing = FastOutSlowInEasing)
                        )).togetherWith(
                            fadeOut(animationSpec = tween(200)) + slideOutHorizontally(
                                animationSpec = tween(380, easing = FastOutSlowInEasing),
                                targetOffsetX = { fullWidth -> fullWidth / 2 }
                            ) + scaleOut(
                                targetScale = 0.90f,
                                transformOrigin = TransformOrigin(1f, 0.5f),
                                animationSpec = tween(380, easing = FastOutSlowInEasing)
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().weight(1f),
                label = "3DPageTurn"
            ) { targetScreen ->
                val activeWords = story.screens.getOrNull(targetScreen)?.split("\\s+".toRegex())?.filter { it.isNotBlank() } ?: emptyList()

                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { cameraDistance = 16f * density }
                        .pointerInput(screenIndex) {
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragOffsetX < -80f) {
                                        if (screenIndex < lastIndex) {
                                            val nextScreen = screenIndex + 1
                                            currentWordIndex = screenProgress[nextScreen] ?: 0
                                            showHelpPrompt = false
                                            screenIndex = nextScreen
                                        } else {
                                            onStoryCompleted(elapsedSeconds, wordsHighlightedCount, hesitationsCount, trickyWords.toList())
                                        }
                                    } else if (dragOffsetX > 80f) {
                                        if (screenIndex > 0) {
                                            showBackChoiceDialog = true
                                        }
                                    }
                                    dragOffsetX = 0f
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount
                                }
                            )
                        },
                    colors = CardDefaults.cardColors(containerColor = pageBgColor),
                    shape = RoundedCornerShape(14.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.2.dp, Color.Gray.copy(alpha = 0.22f))
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 22.dp, vertical = 18.dp)) {
                        if (displayMode == ReadingDisplayMode.PAGE) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(bookScrollState)
                                    .tapOrLongPress(
                                        onTap = {
                                            if (currentWordIndex < words.size) currentWordIndex++
                                        },
                                        onLongPress = { touch ->
                                            meaningController.word = wordAtPosition(layoutHolder.layout, activeWords, touch)
                                        }
                                    )
                            ) {
                                val annotatedText = buildAnnotatedString {
                                    activeWords.forEachIndexed { index, word ->
                                        when {
                                            index < currentWordIndex -> {
                                                withStyle(
                                                    SpanStyle(
                                                        color = pageTextColor,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = fontSizeSp.sp,
                                                        fontFamily = accessibleFont.family,
                                                        background = highlightColor.copy(alpha = 0.35f)
                                                    )
                                                ) {
                                                    append("$word ")
                                                }
                                            }
                                            index == currentWordIndex -> {
                                                withStyle(
                                                    SpanStyle(
                                                        color = pageTextColor,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = fontSizeSp.sp,
                                                        fontFamily = accessibleFont.family,
                                                        background = highlightColor.copy(alpha = glowIntensity),
                                                        textDecoration = TextDecoration.Underline
                                                    )
                                                ) {
                                                    append(word)
                                                }
                                                withStyle(
                                                    SpanStyle(
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = fontSizeSp.sp,
                                                        fontFamily = accessibleFont.family
                                                    )
                                                ) {
                                                    append(" ")
                                                }
                                            }
                                            else -> {
                                                withStyle(
                                                    SpanStyle(
                                                        color = unreadTextColor,
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = fontSizeSp.sp,
                                                        fontFamily = accessibleFont.family
                                                    )
                                                ) {
                                                    append(word)
                                                }
                                                withStyle(
                                                    SpanStyle(
                                                        fontWeight = FontWeight.Medium,
                                                        fontSize = fontSizeSp.sp,
                                                        fontFamily = accessibleFont.family
                                                    )
                                                ) {
                                                    append(" ")
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = annotatedText,
                                    lineHeight = (fontSizeSp * 1.65f).sp,
                                    letterSpacing = 0.2.sp,
                                    onTextLayout = { layoutHolder.layout = it },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Text(
                                    text = "— Page ${screenIndex + 1} of $totalPages — (Swipe left to turn page)",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .tapOrLongPress(
                                        onTap = {
                                            if (currentWordIndex < words.size) currentWordIndex++
                                        },
                                        onLongPress = {
                                            meaningController.word = wordForMeaning(words.getOrNull(currentWordIndex))
                                        }
                                    ),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                if (currentWordIndex < words.size) {
                                    val activeWord = words[currentWordIndex]
                                    val prevWord = if (currentWordIndex > 0) words[currentWordIndex - 1] else ""
                                    val nextWord = if (currentWordIndex + 1 < words.size) words[currentWordIndex + 1] else ""

                                    Text(
                                        text = prevWord,
                                        fontSize = (fontSizeSp - 2).sp,
                                        color = Color.Gray.copy(alpha = 0.6f),
                                        fontFamily = accessibleFont.family,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Card(
                                        modifier = Modifier.fillMaxWidth(0.9f).padding(horizontal = 8.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = highlightColor.copy(alpha = glowIntensity * 0.4f)),
                                        border = BorderStroke(2.dp, highlightColor)
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 16.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = activeWord,
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontFamily = accessibleFont.family,
                                                color = pageTextColor,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Word ${currentWordIndex + 1} of ${words.size}",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "Next: $nextWord (tap anywhere)",
                                        fontSize = (fontSizeSp - 2).sp,
                                        color = unreadTextColor,
                                        fontFamily = accessibleFont.family,
                                        maxLines = 1
                                    )
                                } else {
                                    IconLabel(
                                        text = "Chapter Finished!",
                                        icon = Icons.Filled.EmojiEvents,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = highlightColor
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "— Chapter ${screenIndex + 1} of $totalPages — (Tap anywhere to advance)",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 58.dp),
                colors = CardDefaults.cardColors(containerColor = pageBgColor),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, highlightColor.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentWordIndex < words.size) {
                        val targetWord = words[currentWordIndex]
                        val hint = getPhoneticHint(targetWord)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = targetWord,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = accessibleFont.family,
                                color = pageTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "[$hint]",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = highlightColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilledTonalButton(
                                onClick = {
                                    if (isTtsSpeaking) {
                                        ttsInstance?.stop()
                                        isTtsSpeaking = false
                                    } else {
                                        isTtsSpeaking = true
                                        ttsInstance?.setSpeechRate(0.65f)
                                        ttsInstance?.speak(cleanWord(targetWord), TextToSpeech.QUEUE_FLUSH, null, "hint_audio")
                                        ttsInstance?.setSpeechRate(0.75f)
                                    }
                                },
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = if (isTtsSpeaking) Color(0xFFDC2626).copy(alpha = 0.15f) else highlightColor.copy(alpha = 0.25f),
                                    contentColor = if (isTtsSpeaking) Color(0xFFDC2626) else pageTextColor
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                IconLabel(
                                    text = if (isTtsSpeaking) "Stop" else "Listen",
                                    icon = if (isTtsSpeaking) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedButton(
                                onClick = { showStuckWordDialog = true },
                                border = BorderStroke(1.dp, highlightColor),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                IconLabel("Help", Icons.Filled.Lightbulb, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = pageTextColor)
                            }
                        }
                    } else {
                        Text(
                            text = "Page completed! (tap to finish)",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (currentWordIndex < words.size) currentWordIndex++
                                },
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(pageBgColor, RoundedCornerShape(14.dp))
                .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            if (lastHeardPhrase.isNotBlank() && !isPaused && !isTtsSpeaking) {
                IconLabel(
                    text = "Heard: \"$lastHeardPhrase\"",
                    icon = Icons.Filled.Mic,
                    fontSize = 12.sp,
                    color = highlightColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val dotColor = when {
                    isTtsSpeaking -> Color(0xFF0284C7)
                    isPaused -> Color.Gray
                    else -> highlightColor
                }
                val labelText = when {
                    isTtsSpeaking -> "Pronouncing (Timer paused)..."
                    isPaused -> "Paused (Scroll freely)"
                    else -> "Listening... Read aloud or tap to advance"
                }

                Box(modifier = Modifier.size(7.dp).background(dotColor, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = labelText,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { if (screenIndex > 0) showBackChoiceDialog = true },
                    enabled = screenIndex > 0,
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = pageTextColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    IconLabel("Back", Icons.Filled.ArrowBack, fontSize = 13.sp, maxLines = 1)
                }

                FilledTonalButton(
                    onClick = { showRereadDialog = true },
                    enabled = currentWordIndex > 0,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = highlightColor.copy(alpha = 0.25f)),
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    IconLabel("Re-read", Icons.Filled.Refresh, color = pageTextColor, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
                }

                FilledTonalButton(
                    onClick = { isPaused = !isPaused },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color.Gray.copy(alpha = 0.18f)),
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    IconLabel(if (isPaused) "Resume" else "Pause", if (isPaused) Icons.Filled.PlayArrow else Icons.Filled.Pause, color = pageTextColor, fontSize = 13.sp, maxLines = 1)
                }

                Button(
                    onClick = {
                        if (screenIndex < lastIndex) {
                            val nextScreen = screenIndex + 1
                            currentWordIndex = screenProgress[nextScreen] ?: 0
                            showHelpPrompt = false
                            screenIndex = nextScreen
                        } else {
                            onStoryCompleted(elapsedSeconds, wordsHighlightedCount, hesitationsCount, trickyWords.toList())
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f).height(44.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    IconLabel(
                        text = if (screenIndex < lastIndex) "Next" else "Finish",
                        icon = if (screenIndex < lastIndex) Icons.Filled.ArrowForward else null,
                        iconAtEnd = true,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
