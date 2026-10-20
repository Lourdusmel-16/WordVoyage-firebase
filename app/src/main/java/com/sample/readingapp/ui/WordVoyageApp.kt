package com.sample.readingapp.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.sample.readingapp.data.*
import com.sample.readingapp.speech.*
import com.sample.readingapp.theme.*
import com.sample.readingapp.ui.auth.*
import com.sample.readingapp.ui.home.*
import com.sample.readingapp.ui.intro.*
import com.sample.readingapp.ui.reader.*
import com.sample.readingapp.util.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun WordVoyageApp(auth: FirebaseAuth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        SpeechEngineCache.initTts(context)
        SpeechEngineCache.prewarmSherpa(context)
    }

    var appState by remember { mutableStateOf(AppState.SPLASH) }
    var stories by remember { mutableStateOf<List<Story>>(emptyList()) }
    var isLoadingStories by remember { mutableStateOf(true) }
    var isAdminUser by remember { mutableStateOf(false) }

    var currentUsername by remember { mutableStateOf("Reader") }
    var currentAgeGroup by remember { mutableStateOf(AgeGroup.GRADES_4_5) }

    val playedStoryIds = remember { mutableStateListOf<String>() }
    val completedStoriesMap = remember { mutableStateMapOf<String, CompletedStoryRecord>() }
    val storyPagesProgress = remember { mutableStateMapOf<String, Int>() }
    val storyWordsProgress = remember { mutableStateMapOf<String, Int>() }
    // LAST WORD REACHED ON EVERY PAGE (KEY = pageWordKey(storyId, pageIndex))
    val pageWordProgress = remember { mutableStateMapOf<String, Int>() }

    fun pickNextRandomStory(): Story? {
        if (stories.isEmpty()) return null
        val unplayed = stories.filter { it.id !in playedStoryIds }
        return unplayed.randomOrNull()
    }

    // RESET PROGRESS FUNCTION (CURRENT GRADE VS ALL GRADES) - ALSO RESETS FIREBASE TO NULL (DEFAULT)
    fun handleResetProgress(forAllGrades: Boolean) {
        val removedStoryIds: Set<String>
        val removedPageWordKeys: List<String>

        if (forAllGrades) {
            removedStoryIds = emptySet()
            removedPageWordKeys = emptyList()
            playedStoryIds.clear()
            completedStoriesMap.clear()
            storyPagesProgress.clear()
            storyWordsProgress.clear()
            pageWordProgress.clear()
        } else {
            val currentIds = stories.map { it.id }.toSet()
            removedStoryIds = currentIds
            removedPageWordKeys = pageWordProgress.keys.filter { key ->
                currentIds.any { id -> key.startsWith("${id}__p") }
            }
            playedStoryIds.removeAll(currentIds)
            currentIds.forEach { id ->
                completedStoriesMap.remove(id)
                storyPagesProgress.remove(id)
                storyWordsProgress.remove(id)
            }
            removedPageWordKeys.forEach { pageWordProgress.remove(it) }
        }

        val remainingPlayedEmpty = playedStoryIds.isEmpty()
        val remainingCompletedEmpty = completedStoriesMap.isEmpty()
        val remainingPagesEmpty = storyPagesProgress.isEmpty()
        val remainingWordsEmpty = storyWordsProgress.isEmpty()
        val remainingPageWordsEmpty = pageWordProgress.isEmpty()

        auth.currentUser?.let { u ->
            val userRef = FirebaseFirestore.getInstance().collection("users").document(u.uid)
            scope.launch {
                try {
                    if (forAllGrades) {
                        userRef.set(
                            mapOf<String, Any?>(
                                "playedStoryIds" to null,
                                "storyPagesProgress" to null,
                                "storyWordsProgress" to null,
                                "completedStories" to null,
                                "pageWordProgress" to null
                            ),
                            SetOptions.merge()
                        ).await()
                    } else {
                        val updates = hashMapOf<String, Any?>()
                        removedStoryIds.forEach { id ->
                            updates["storyPagesProgress.$id"] = FieldValue.delete()
                            updates["storyWordsProgress.$id"] = FieldValue.delete()
                            updates["completedStories.$id"] = FieldValue.delete()
                        }
                        removedPageWordKeys.forEach { key ->
                            updates["pageWordProgress.$key"] = FieldValue.delete()
                        }
                        if (removedStoryIds.isNotEmpty()) {
                            updates["playedStoryIds"] = FieldValue.arrayRemove(*removedStoryIds.toTypedArray())
                        }
                        if (updates.isNotEmpty()) {
                            userRef.update(updates).await()
                        }

                        // ANY FIELD THAT IS NOW EMPTY GOES BACK TO NULL (DEFAULT)
                        val nullFields = hashMapOf<String, Any?>()
                        if (remainingPlayedEmpty) nullFields["playedStoryIds"] = null
                        if (remainingCompletedEmpty) nullFields["completedStories"] = null
                        if (remainingPagesEmpty) nullFields["storyPagesProgress"] = null
                        if (remainingWordsEmpty) nullFields["storyWordsProgress"] = null
                        if (remainingPageWordsEmpty) nullFields["pageWordProgress"] = null
                        if (nullFields.isNotEmpty()) {
                            userRef.set(nullFields, SetOptions.merge()).await()
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // ALWAYS WITHDRAW READ HISTORY / PROGRESS FROM FIREBASE (LOCAL STATE IS REBUILT FROM FIRESTORE)
    suspend fun loadProgressFromFirebase() {
        val user = auth.currentUser ?: return
        try {
            val userDoc = FirebaseFirestore.getInstance().collection("users").document(user.uid).get().await()
            currentUsername = userDoc.getString("username") ?: user.email?.substringBefore("@") ?: "Reader"
            currentAgeGroup = AgeGroup.fromString(userDoc.getString("ageGroup"))

            playedStoryIds.clear()
            completedStoriesMap.clear()
            storyPagesProgress.clear()
            storyWordsProgress.clear()
            pageWordProgress.clear()

            val savedReadIds = userDoc.get("playedStoryIds") as? List<*>
            savedReadIds?.filterIsInstance<String>()?.forEach { id ->
                if (id !in playedStoryIds) playedStoryIds.add(id)
            }

            val savedCompletedMap = userDoc.get("completedStories") as? Map<*, *>
            savedCompletedMap?.forEach { (id, data) ->
                if (id is String && data is Map<*, *>) {
                    completedStoriesMap[id] = CompletedStoryRecord(
                        id = id,
                        title = data["title"] as? String ?: "Story",
                        gradeLevel = data["gradeLevel"] as? String ?: "",
                        totalPages = (data["totalPages"] as? Number)?.toInt() ?: 1,
                        completedAt = (data["completedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
            }

            val savedPages = userDoc.get("storyPagesProgress") as? Map<*, *>
            savedPages?.forEach { (k, v) ->
                if (k is String && v is Number) {
                    storyPagesProgress[k] = v.toInt()
                }
            }
            val savedWords = userDoc.get("storyWordsProgress") as? Map<*, *>
            savedWords?.forEach { (k, v) ->
                if (k is String && v is Number) {
                    storyWordsProgress[k] = v.toInt()
                }
            }
            val savedPageWords = userDoc.get("pageWordProgress") as? Map<*, *>
            savedPageWords?.forEach { (k, v) ->
                if (k is String && v is Number) {
                    pageWordProgress[k] = v.toInt()
                }
            }
        } catch (_: Exception) {}
    }

    LaunchedEffect(auth.currentUser) {
        val user = auth.currentUser
        if (user != null) {
            loadProgressFromFirebase()
            isLoadingStories = true
            stories = StoryRepository.fetchStoriesByAgeGroup(currentAgeGroup)
            isLoadingStories = false
        }
    }

    var currentStory by remember { mutableStateOf<Story?>(null) }
    var totalTimeSeconds by remember { mutableStateOf(0) }
    var totalWordsHighlighted by remember { mutableStateOf(0) }
    var totalHesitations by remember { mutableStateOf(0) }
    var trickyWordsList by remember { mutableStateOf(listOf<String>()) }

    var selectedHighlightColor by remember { mutableStateOf(HighlightPalettes[0].first) }
    var readerTheme by remember { mutableStateOf(ReaderTheme.SEPIA) }
    var displayMode by remember { mutableStateOf(ReadingDisplayMode.PAGE) }
    var accessibleFont by remember { mutableStateOf(AccessibleFont.SERIF) }
    var fontSizeSp by remember { mutableStateOf(19) }
    SideEffect {
        GuideSession.onReplay = {
            val pick = stories.randomOrNull()
            if (pick != null) {
                GuideSession.replayRequested = true
                currentStory = pick
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    SpeechEngineCache.cachedSherpaManager?.startListening()
                }
                appState = AppState.READING
            }
        }
    }
    DisposableEffect(Unit) { onDispose { GuideSession.onReplay = null } }

    val pageBgColor = when (readerTheme) {
        ReaderTheme.SEPIA -> Color(0xFFFBF4E8)
        ReaderTheme.DAY -> Color(0xFFFFFFFF)
        ReaderTheme.NIGHT -> Color(0xFF181A1D)
    }

    val pageTextColor = when (readerTheme) {
        ReaderTheme.SEPIA -> Color(0xFF2C241E)
        ReaderTheme.DAY -> Color(0xFF1A1A1A)
        ReaderTheme.NIGHT -> Color(0xFFE2E8F0)
    }

    val scaffoldBgColor = when (readerTheme) {
        ReaderTheme.SEPIA -> Color(0xFFEFE7D8)
        ReaderTheme.DAY -> Color(0xFFF1F5F9)
        ReaderTheme.NIGHT -> Color(0xFF0F1115)
    }

    Box(modifier = Modifier.fillMaxSize().background(scaffoldBgColor)) {
        AnimatedContent(
            targetState = appState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(380)) + scaleIn(initialScale = 0.95f))
                    .togetherWith(fadeOut(animationSpec = tween(280)) + scaleOut(targetScale = 1.02f))
            },
            label = "AppStateTransition"
        ) { targetState ->
            when (targetState) {
                AppState.SPLASH -> {
                    SplashScreen(
                        onSplashFinished = {
                            appState = if (auth.currentUser != null) AppState.HOME_INITIAL else AppState.AUTH
                        }
                    )
                }

                AppState.AUTH -> {
                    AuthScreen(
                        auth = auth,
                        pageBgColor = pageBgColor,
                        pageTextColor = pageTextColor,
                        accentColor = BrandShadeBold,
                        readerTheme = readerTheme,
                        onThemeChanged = { readerTheme = it },
                        onLoginSuccess = { isAdmin, username, ageGroup ->
                            isAdminUser = isAdmin
                            currentUsername = username
                            currentAgeGroup = ageGroup
                            scope.launch {
                                isLoadingStories = true
                                loadProgressFromFirebase()
                                stories = StoryRepository.fetchStoriesByAgeGroup(ageGroup)
                                isLoadingStories = false
                            }
                            appState = AppState.HOME_INITIAL
                        },
                        onRegisterSuccess = { isAdmin, username, ageGroup ->
                            isAdminUser = isAdmin
                            currentUsername = username
                            currentAgeGroup = ageGroup
                            scope.launch {
                                isLoadingStories = true
                                stories = StoryRepository.fetchStoriesByAgeGroup(ageGroup)
                                isLoadingStories = false
                            }
                            appState = AppState.WELCOME_INTRO
                        }
                    )
                }

                AppState.WELCOME_INTRO -> {
                    WelcomeIntroScreen(
                        username = currentUsername,
                        ageGroup = currentAgeGroup,
                        pageBgColor = pageBgColor,
                        pageTextColor = pageTextColor,
                        accentColor = BrandShadeBold,
                        onContinue = {
                            appState = AppState.HOME_INITIAL
                        }
                    )
                }

                AppState.HOME_INITIAL -> {
                    InitialHomeScreen(
                        isLoading = isLoadingStories,
                        username = currentUsername,
                        ageGroup = currentAgeGroup,
                        isAdmin = isAdminUser,
                        stories = stories,
                        playedStoryIds = playedStoryIds,
                        completedStoriesMap = completedStoriesMap,
                        storyPagesProgress = storyPagesProgress,
                        allStoriesRead = stories.isNotEmpty() && stories.all { it.id in playedStoryIds },
                        storiesCount = stories.size,
                        selectedColor = selectedHighlightColor,
                        readerTheme = readerTheme,
                        pageBgColor = pageBgColor,
                        pageTextColor = pageTextColor,
                        onThemeChanged = { readerTheme = it },
                        onColorSelected = { selectedHighlightColor = it },
                        onSelectStory = { selectedStory ->
                            currentStory = selectedStory
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                SpeechEngineCache.cachedSherpaManager?.startListening()
                            }
                            appState = AppState.READING
                        },
                        onUpdateProfile = { newName, newGroup ->
                            currentUsername = newName
                            currentAgeGroup = newGroup
                            auth.currentUser?.let { user ->
                                FirebaseFirestore.getInstance().collection("users").document(user.uid)
                                    .set(mapOf("username" to newName, "ageGroup" to newGroup.name), SetOptions.merge())
                            }
                            scope.launch {
                                isLoadingStories = true
                                stories = StoryRepository.fetchStoriesByAgeGroup(newGroup)
                                isLoadingStories = false
                            }
                        },
                        onRefreshStories = {
                            isLoadingStories = true
                            stories = StoryRepository.fetchStoriesByAgeGroup(currentAgeGroup)
                            isLoadingStories = false
                        },
                        onResetProgress = { forAllGrades ->
                            handleResetProgress(forAllGrades)
                        },
                        onStartReading = {
                            val next = pickNextRandomStory()
                            if (next != null) {
                                currentStory = next
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    SpeechEngineCache.cachedSherpaManager?.startListening()
                                }
                                appState = AppState.READING
                            }
                        },
                        onLogout = {
                            auth.signOut()
                            currentStory = null
                            stories = emptyList()
                            playedStoryIds.clear()
                            completedStoriesMap.clear()
                            storyPagesProgress.clear()
                            storyWordsProgress.clear()
                            pageWordProgress.clear()
                            isAdminUser = false
                            appState = AppState.AUTH
                        }
                    )
                }

                AppState.READING -> {
                    currentStory?.let { story ->
                        val isCompleted = story.id in playedStoryIds
                        val savedPage = storyPagesProgress[story.id] ?: 0
                        val savedWord = storyWordsProgress[story.id] ?: 0

                        val initialPage = if (isCompleted || savedPage >= story.screens.size) 0 else savedPage.coerceIn(0, (story.screens.size - 1).coerceAtLeast(0))
                        // RESUME FROM THE LAST WORD REMEMBERED FOR THAT PAGE (FALLS BACK TO THE OLD STORY-LEVEL WORD)
                        val initialWord = if (isCompleted || savedPage >= story.screens.size) 0
                        else (pageWordProgress[pageWordKey(story.id, initialPage)] ?: savedWord)

                        // LAST WORD REMEMBERED FOR EVERY PAGE OF THIS STORY (EMPTY WHEN RE-READING A COMPLETED STORY)
                        val savedPageWords: Map<Int, Int> = if (isCompleted) {
                            emptyMap()
                        } else {
                            (0 until story.screens.size).mapNotNull { p ->
                                pageWordProgress[pageWordKey(story.id, p)]?.let { w -> p to w }
                            }.toMap()
                        }

                        ReadingScreen(
                            story = story,
                            initialScreenIndex = initialPage,
                            initialWordIndex = initialWord,
                            savedPageWords = savedPageWords,
                            username = currentUsername,
                            highlightColor = selectedHighlightColor,
                            onHighlightColorChanged = { selectedHighlightColor = it },
                            readerTheme = readerTheme,
                            pageBgColor = pageBgColor,
                            pageTextColor = pageTextColor,
                            fontSizeSp = fontSizeSp,
                            accessibleFont = accessibleFont,
                            displayMode = displayMode,
                            onDisplayModeChanged = { displayMode = it },
                            onThemeChanged = { readerTheme = it },
                            onFontSizeChanged = { fontSizeSp = it },
                            onFontFamilyChanged = { accessibleFont = it },
                            onProgressChanged = { pageIndex, wordIndex ->
                                storyPagesProgress[story.id] = pageIndex
                                storyWordsProgress[story.id] = wordIndex
                                pageWordProgress[pageWordKey(story.id, pageIndex)] = wordIndex
                                auth.currentUser?.let { u ->
                                    FirebaseFirestore.getInstance().collection("users").document(u.uid)
                                        .set(
                                            mapOf(
                                                "storyPagesProgress" to storyPagesProgress.toMap(),
                                                "storyWordsProgress" to storyWordsProgress.toMap(),
                                                "pageWordProgress" to pageWordProgress.toMap()
                                            ),
                                            SetOptions.merge()
                                        )
                                }
                            },
                            onExitToHome = {
                                appState = AppState.HOME_INITIAL
                            },
                            onStoryCompleted = { elapsed, wordsCount, hesitationsCount, trickyWords ->
                                totalTimeSeconds = elapsed
                                totalWordsHighlighted = wordsCount
                                totalHesitations = hesitationsCount
                                trickyWordsList = trickyWords
                                if (story.id !in playedStoryIds) {
                                    playedStoryIds.add(story.id)
                                }
                                storyPagesProgress[story.id] = story.screens.size
                                storyWordsProgress[story.id] = 0

                                val record = CompletedStoryRecord(
                                    id = story.id,
                                    title = story.title,
                                    gradeLevel = story.gradeLevel,
                                    totalPages = story.screens.size,
                                    completedAt = System.currentTimeMillis()
                                )
                                completedStoriesMap[story.id] = record

                                auth.currentUser?.let { u ->
                                    FirebaseFirestore.getInstance().collection("users").document(u.uid)
                                        .set(
                                            mapOf(
                                                "playedStoryIds" to playedStoryIds.toList(),
                                                "storyPagesProgress" to storyPagesProgress.toMap(),
                                                "storyWordsProgress" to storyWordsProgress.toMap(),
                                                "pageWordProgress" to pageWordProgress.toMap(),
                                                "completedStories" to completedStoriesMap.mapValues { (_, r) ->
                                                    mapOf(
                                                        "title" to r.title,
                                                        "gradeLevel" to r.gradeLevel,
                                                        "totalPages" to r.totalPages,
                                                        "completedAt" to r.completedAt
                                                    )
                                                }
                                            ),
                                            SetOptions.merge()
                                        )
                                }
                                appState = AppState.CELEBRATION
                            }
                        )
                    }
                }

                AppState.CELEBRATION -> {
                    CelebrationScreen(
                        username = currentUsername,
                        highlightColor = selectedHighlightColor,
                        onFinished = {
                            appState = AppState.COMPLETED
                        }
                    )
                }

                AppState.COMPLETED -> {
                    currentStory?.let { completedStory ->
                        CompletionScreen(
                            story = completedStory,
                            username = currentUsername,
                            ageGroup = currentAgeGroup,
                            totalSeconds = totalTimeSeconds,
                            totalWordsRead = totalWordsHighlighted,
                            hesitationsCount = totalHesitations,
                            trickyWords = trickyWordsList,
                            highlightColor = selectedHighlightColor,
                            pageBgColor = pageBgColor,
                            pageTextColor = pageTextColor,
                            readerTheme = readerTheme,
                            stories = stories,
                            playedStoryIds = playedStoryIds,
                            completedStoriesMap = completedStoriesMap,
                            storyPagesProgress = storyPagesProgress,
                            allStoriesRead = stories.isNotEmpty() && stories.all { it.id in playedStoryIds },
                            onThemeChanged = { readerTheme = it },
                            onUpdateProfile = { newName, newGroup ->
                                currentUsername = newName
                                currentAgeGroup = newGroup
                                auth.currentUser?.let { user ->
                                    FirebaseFirestore.getInstance().collection("users").document(user.uid)
                                        .set(mapOf("username" to newName, "ageGroup" to newGroup.name), SetOptions.merge())
                                }
                                scope.launch {
                                    isLoadingStories = true
                                    stories = StoryRepository.fetchStoriesByAgeGroup(newGroup)
                                    isLoadingStories = false
                                }
                            },
                            onResetProgress = { forAllGrades ->
                                handleResetProgress(forAllGrades)
                            },
                            onSelectStory = { nextStory ->
                                currentStory = nextStory
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    SpeechEngineCache.cachedSherpaManager?.startListening()
                                }
                                appState = AppState.READING
                            },
                            onSelectAdaptiveStory = {
                                val next = pickNextRandomStory()
                                if (next != null) {
                                    currentStory = next
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        SpeechEngineCache.cachedSherpaManager?.startListening()
                                    }
                                    appState = AppState.READING
                                } else {
                                    appState = AppState.HOME_INITIAL
                                }
                            },
                            onExitToFirstScreen = {
                                appState = AppState.HOME_INITIAL
                            },
                            onLogout = {
                                auth.signOut()
                                currentStory = null
                                stories = emptyList()
                                playedStoryIds.clear()
                                completedStoriesMap.clear()
                                storyPagesProgress.clear()
                                storyWordsProgress.clear()
                                pageWordProgress.clear()
                                isAdminUser = false
                                appState = AppState.AUTH
                            }
                        )
                    }
                }
            }
        }
    }
}
