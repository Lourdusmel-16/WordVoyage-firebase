package com.sample.readingapp.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.window.Dialog
import com.sample.readingapp.data.*
import com.sample.readingapp.theme.*
import com.sample.readingapp.ui.components.*

// ---------------- COMPLETION SCREEN ----------------
@Composable
fun CompletionScreen(
    story: Story,
    username: String,
    ageGroup: AgeGroup,
    totalSeconds: Int,
    totalWordsRead: Int,
    hesitationsCount: Int,
    trickyWords: List<String>,
    highlightColor: Color,
    pageBgColor: Color,
    pageTextColor: Color,
    readerTheme: ReaderTheme,
    stories: List<Story>,
    playedStoryIds: List<String>,
    completedStoriesMap: Map<String, CompletedStoryRecord>,
    storyPagesProgress: Map<String, Int>,
    allStoriesRead: Boolean,
    onThemeChanged: (ReaderTheme) -> Unit,
    onUpdateProfile: (String, AgeGroup) -> Unit,
    onResetProgress: (forAllGrades: Boolean) -> Unit,
    onSelectStory: (Story) -> Unit,
    onSelectAdaptiveStory: () -> Unit,
    onExitToFirstScreen: () -> Unit,
    onLogout: () -> Unit
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var showExitHomeConfirmDialog by remember { mutableStateOf(false) }
    var showResetProgressPrompt by remember { mutableStateOf(false) }

    // RESET FLOW STATE: YES/NO BOX FIRST, THEN (FROM PROFILE ONLY) "SAVE CHANGES & RESET"
    var showResetConfirmYesNo by remember { mutableStateOf(false) }
    var resetFromProfile by remember { mutableStateOf(false) }
    var profileResetPending by remember { mutableStateOf(false) }
    var profileResetAllGrades by remember { mutableStateOf(false) }
    var resetScopeAll by remember { mutableStateOf(false) }

    BackHandler {
        showExitHomeConfirmDialog = true
    }

    // YES / NO CONFIRMATION BEFORE ANY RESET
    if (showResetConfirmYesNo) {
        ResetYesNoDialog(
            pageBgColor = pageBgColor,
            pageTextColor = pageTextColor,
            onYes = {
                showResetConfirmYesNo = false
                if (resetFromProfile) {
                    profileResetAllGrades = resetScopeAll
                    profileResetPending = true
                    resetFromProfile = false
                } else {
                    onResetProgress(resetScopeAll)
                }
            },
            onNo = {
                showResetConfirmYesNo = false
                resetFromProfile = false
            }
        )
    }

    // PROMPT DIALOG: RESET FOR CURRENT GRADE OR EVERY GRADE
    if (showResetProgressPrompt) {
        ResetScopeDialog(
            ageGroup = ageGroup,
            pageBgColor = pageBgColor,
            pageTextColor = pageTextColor,
            onDismiss = {
                showResetProgressPrompt = false
                resetFromProfile = false
            },
            onResetCurrentGrade = {
                showResetProgressPrompt = false
                resetScopeAll = false
                showResetConfirmYesNo = true
            },
            onResetAllGrades = {
                showResetProgressPrompt = false
                resetScopeAll = true
                showResetConfirmYesNo = true
            },
            onCancel = {
                showResetProgressPrompt = false
                resetFromProfile = false
            }
        )
    }

    if (showExitHomeConfirmDialog) {
        Dialog(onDismissRequest = { showExitHomeConfirmDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBgColor, RoundedCornerShape(18.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Exit to Library?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pageTextColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Are you sure you want to exit and return to the story library?", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showExitHomeConfirmDialog = false },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("Cancel", color = pageTextColor, fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = {
                                showExitHomeConfirmDialog = false
                                onExitToFirstScreen()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("Yes, Exit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }

    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val timeFormatted = String.format("%02d:%02d", minutes, seconds)
    val finalWpm = if (totalSeconds > 0) ((totalWordsRead * 60) / totalSeconds).coerceIn(0, 240) else 0

    var selectedOptionIndex by remember { mutableStateOf<Int?>(null) }
    var quizSubmitted by remember { mutableStateOf(false) }

    val remainingStories = stories.filter {
        it.id != story.id && (it.ageGroup == ageGroup.name || it.gradeLevel.contains(ageGroup.badge, ignoreCase = true))
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            ScreenTopNavigationBar(
                username = username,
                ageGroup = ageGroup,
                isAdmin = false,
                readerTheme = readerTheme,
                pageTextColor = pageTextColor,
                onProfileClick = { showProfileDialog = true },
                onResetProgressClick = { showResetProgressPrompt = true },
                onThemeChanged = onThemeChanged,
                onLogout = onLogout
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    IconLabel("Fantastic Job, $username!", Icons.Filled.EmojiEvents, iconTint = Color(0xFFF59E0B), iconAtEnd = true, fontSize = 23.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = pageTextColor)
                    Text("Story: ${story.title} • ${story.gradeLevel}", fontSize = 13.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = pageBgColor),
                        border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.25f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "$totalWordsRead", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = highlightColor)
                                    Text(text = "Words Spoken", fontSize = 12.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "$finalWpm WPM", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = highlightColor)
                                    Text(text = "Fluency Speed", fontSize = 12.sp, color = Color.Gray)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = timeFormatted, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = pageTextColor)
                                    Text(text = "Reading Time", fontSize = 12.sp, color = Color.Gray)
                                }
                            }

                            if (trickyWords.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Divider(color = Color.Gray.copy(alpha = 0.2f))
                                Spacer(modifier = Modifier.height(8.dp))
                                IconLabel(
                                    text = "Hesitation Words: ${trickyWords.take(4).joinToString(", ")}",
                                    icon = Icons.Filled.Lightbulb,
                                    fontSize = 12.sp,
                                    color = Color(0xFFDC2626)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = pageBgColor),
                        border = BorderStroke(1.dp, highlightColor.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            IconLabel("Micro-Comprehension Check", Icons.Filled.Psychology, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = pageTextColor)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = story.quiz.question, fontSize = 13.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(12.dp))

                            story.quiz.options.forEachIndexed { index, option ->
                                val isSelected = selectedOptionIndex == index
                                val isCorrect = index == story.quiz.correctIndex
                                val btnColor = when {
                                    quizSubmitted && isCorrect -> Color(0xFF10B981)
                                    quizSubmitted && isSelected && !isCorrect -> Color(0xFFEF4444)
                                    isSelected -> highlightColor
                                    else -> Color.Transparent
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (!quizSubmitted) {
                                            selectedOptionIndex = index
                                            quizSubmitted = true
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (btnColor != Color.Transparent) btnColor.copy(alpha = 0.2f) else Color.Transparent,
                                        contentColor = pageTextColor
                                    ),
                                    border = BorderStroke(1.dp, if (btnColor != Color.Transparent) btnColor else Color.Gray.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Text(text = option, fontSize = 13.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                                }
                            }

                            if (quizSubmitted) {
                                Spacer(modifier = Modifier.height(6.dp))
                                val isRight = selectedOptionIndex == story.quiz.correctIndex
                                IconLabel(
                                    text = if (isRight) "Correct! Superb comprehension." else "Notice: The correct answer was \"${story.quiz.options.getOrNull(story.quiz.correctIndex) ?: ""}\"",
                                    icon = if (isRight) Icons.Filled.CheckCircle else null,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRight) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (allStoriesRead) {
                                showExitHomeConfirmDialog = true
                            } else {
                                onSelectAdaptiveStory()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = highlightColor),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        IconLabel(
                            text = if (allStoriesRead) "Return to Home (All Read)" else "Next Story in ${ageGroup.badge}",
                            icon = if (allStoriesRead) Icons.Filled.Home else Icons.Filled.TrendingUp,
                            fontSize = 15.sp,
                            color = Color(0xFF1E293B),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showExitHomeConfirmDialog = true },
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) {
                        IconLabel("Exit to Home Library", Icons.Filled.ExitToApp, iconAtEnd = true, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text("More Adventures in ${ageGroup.title}", fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = pageTextColor)
                    Spacer(modifier = Modifier.height(10.dp))
                }

                items(remainingStories) { otherStory ->
                    val isRead = otherStory.id in playedStoryIds
                    val otherTotalPages = otherStory.screens.size.coerceAtLeast(1)
                    val otherPagesRead = if (isRead) otherTotalPages else (storyPagesProgress[otherStory.id] ?: 0).coerceIn(0, otherTotalPages)
                    val otherIsStarted = isRead || storyPagesProgress.containsKey(otherStory.id)

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable { onSelectStory(otherStory) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = pageBgColor),
                        border = BorderStroke(
                            1.dp,
                            if (isRead) Color(0xFF10B981).copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.25f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp, 60.dp)
                                    .background(
                                        if (isRead) Color(0xFF10B981).copy(alpha = 0.15f) else highlightColor.copy(alpha = 0.35f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isRead) Color(0xFF10B981) else highlightColor.copy(alpha = 0.6f),
                                        RoundedCornerShape(4.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                imageVector = if (isRead) Icons.Filled.CheckCircle else Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = if (isRead) Color(0xFF10B981) else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(otherStory.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = pageTextColor)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("${otherStory.difficulty} • ${otherStory.gradeLevel}", fontSize = 12.sp, color = Color.Gray)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    IconLabel(
                                        text = if (isRead) "Completed" else if (otherIsStarted) "In Progress" else "Unread",
                                        icon = if (isRead) Icons.Filled.CheckCircle else if (otherIsStarted) Icons.Filled.PlayArrow else Icons.Filled.RadioButtonUnchecked,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isRead) Color(0xFF10B981) else if (otherIsStarted) BrandShadeMedium else Color(0xFF64748B)
                                    )
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LinearProgressIndicator(
                                        progress = otherPagesRead.toFloat() / otherTotalPages,
                                        modifier = Modifier.weight(1f).height(6.dp),
                                        color = if (isRead) Color(0xFF10B981) else highlightColor,
                                        trackColor = Color.Gray.copy(alpha = 0.2f)
                                    )
                                    Text(
                                        text = if (!isRead && otherIsStarted) "On page ${(otherPagesRead + 1).coerceAtMost(otherTotalPages)} of $otherTotalPages" else "$otherPagesRead / $otherTotalPages pages read",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isRead) Color(0xFF10B981) else Color.Gray
                                    )
                                }
                            }

                            Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = highlightColor, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (showProfileDialog) {
            ReaderDashboardDialog(
                username = username,
                ageGroup = ageGroup,
                stories = stories,
                playedStoryIds = playedStoryIds,
                completedStoriesMap = completedStoriesMap,
                pageBgColor = pageBgColor,
                pageTextColor = pageTextColor,
                isResetPending = profileResetPending,
                onClose = {
                    showProfileDialog = false
                    profileResetPending = false
                },
                onSave = { newName, newGroup ->
                    if (profileResetPending) {
                        onResetProgress(profileResetAllGrades)
                        profileResetPending = false
                    }
                    onUpdateProfile(newName, newGroup)
                    showProfileDialog = false
                },
                onRequestReset = {
                    resetFromProfile = true
                    showResetProgressPrompt = true
                },
                onSelectStory = onSelectStory,
                onLogout = onLogout
            )
        }
    }
}
