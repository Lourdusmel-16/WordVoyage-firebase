package com.sample.readingapp.ui.home

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sample.readingapp.data.*
import com.sample.readingapp.theme.*
import com.sample.readingapp.ui.components.*
import com.sample.readingapp.util.*
import kotlinx.coroutines.launch

// ---------------- 1. HOME SCREEN WITH RESET PROGRESS CAPABILITY ----------------
@Composable
fun InitialHomeScreen(
    isLoading: Boolean,
    username: String,
    ageGroup: AgeGroup,
    isAdmin: Boolean,
    stories: List<Story>,
    playedStoryIds: List<String>,
    completedStoriesMap: Map<String, CompletedStoryRecord>,
    storyPagesProgress: Map<String, Int>,
    allStoriesRead: Boolean,
    storiesCount: Int,
    selectedColor: Color,
    readerTheme: ReaderTheme,
    pageBgColor: Color,
    pageTextColor: Color,
    onThemeChanged: (ReaderTheme) -> Unit,
    onColorSelected: (Color) -> Unit,
    onSelectStory: (Story) -> Unit,
    onUpdateProfile: (String, AgeGroup) -> Unit,
    onRefreshStories: suspend () -> Unit,
    onResetProgress: (forAllGrades: Boolean) -> Unit,
    onStartReading: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showUploadDialog by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }
    var showExitAppConfirm by remember { mutableStateOf(false) }
    var showResetProgressPrompt by remember { mutableStateOf(false) }

    // RESET FLOW STATE: YES/NO BOX FIRST, THEN (FROM PROFILE ONLY) "SAVE CHANGES & RESET"
    var showResetConfirmYesNo by remember { mutableStateOf(false) }
    var resetFromProfile by remember { mutableStateOf(false) }
    var profileResetPending by remember { mutableStateOf(false) }
    var profileResetAllGrades by remember { mutableStateOf(false) }
    var resetScopeAll by remember { mutableStateOf(false) }

    BackHandler {
        showExitAppConfirm = true
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

    // PROMPT DIALOG: RESET FOR PARTICULAR GRADE OR EVERY GRADE
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

    if (showExitAppConfirm) {
        Dialog(onDismissRequest = { showExitAppConfirm = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBgColor, RoundedCornerShape(18.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Exit App?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pageTextColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Are you sure you want to exit the app now?", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showExitAppConfirm = false },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("Cancel", color = pageTextColor, fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = {
                                showExitAppConfirm = false
                                (context as? Activity)?.finish()
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

    Column(modifier = Modifier.fillMaxSize()) {
        ScreenTopNavigationBar(
            username = username,
            ageGroup = ageGroup,
            isAdmin = isAdmin,
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(pageBgColor, RoundedCornerShape(22.dp))
                        .border(1.dp, Color.Gray.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(APP_DISPLAY_NAME, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Serif, color = pageTextColor)
                    Text("Grade Level: ${ageGroup.title} • Stories: $storiesCount", fontSize = 12.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(14.dp))

                    if (allStoriesRead) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                            border = BorderStroke(1.dp, Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                IconLabel("All Stories for ${ageGroup.badge} Completed!", Icons.Filled.EmojiEvents, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF047857))
                                Text("Reset your journey or switch grade levels in your profile.", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(onClick = { showResetProgressPrompt = true }) {
                                    IconLabel("Reset Progress", Icons.Filled.Refresh, color = Color(0xFF047857), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    Text("Reading Highlighter:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HighlightPalettes.forEach { (color, _) ->
                            val isSelected = color == selectedColor
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(color, CircleShape)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) pageTextColor else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { onColorSelected(color) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isAdmin) {
                        Button(
                            onClick = { showUploadDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                        ) {
                            IconLabel("Upload New Story (Admin)", Icons.Filled.Add, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = { scope.launch { onRefreshStories() } },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                        ) {
                            IconLabel("Refresh from Firebase", Icons.Filled.Refresh, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    Button(
                        onClick = onStartReading,
                        enabled = !isLoading && storiesCount > 0,
                        colors = ButtonDefaults.buttonColors(containerColor = selectedColor),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF1E293B))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Preparing your stories...", fontSize = 15.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                        } else {
                            IconLabel("Start Reading Journey", Icons.Filled.PlayArrow, iconAtEnd = true, fontSize = 16.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                Text(
                    text = "Library Stories (${stories.count { it.id in playedStoryIds }}/$storiesCount Read)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    color = pageTextColor,
                    modifier = Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 8.dp)
                )
            }

            items(stories) { storyItem ->
                val isRead = storyItem.id in playedStoryIds
                val totalPages = storyItem.screens.size.coerceAtLeast(1)
                val pagesRead = if (isRead) totalPages else (storyPagesProgress[storyItem.id] ?: 0).coerceIn(0, totalPages)
                val isStarted = isRead || storyPagesProgress.containsKey(storyItem.id)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onSelectStory(storyItem) },
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = pageBgColor),
                    border = BorderStroke(
                        1.2.dp,
                        if (isRead) Color(0xFF10B981).copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.25f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    if (isRead) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.1f),
                                    CircleShape
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

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = storyItem.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = pageTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${storyItem.difficulty} • ${storyItem.gradeLevel}",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                IconLabel(
                                    text = if (isRead) "Completed" else if (isStarted) "In Progress" else "Unread",
                                    icon = if (isRead) Icons.Filled.CheckCircle else if (isStarted) Icons.Filled.PlayArrow else Icons.Filled.RadioButtonUnchecked,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRead) Color(0xFF10B981) else if (isStarted) BrandShadeMedium else Color(0xFF64748B)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                LinearProgressIndicator(
                                    progress = pagesRead.toFloat() / totalPages,
                                    modifier = Modifier.weight(1f).height(6.dp),
                                    color = if (isRead) Color(0xFF10B981) else selectedColor,
                                    trackColor = Color.Gray.copy(alpha = 0.2f)
                                )
                                Text(
                                    text = if (!isRead && isStarted) "On page ${(pagesRead + 1).coerceAtMost(totalPages)} of $totalPages" else "$pagesRead / $totalPages pages read",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isRead) Color(0xFF10B981) else Color.Gray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = selectedColor, modifier = Modifier.size(20.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // PROFILE DASHBOARD (WITH RESET PROGRESS BUTTON INCLUDED)
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

    if (showUploadDialog) {
        var jsonText by remember { mutableStateOf("") }
        var uploadTargetAgeGroup by remember { mutableStateOf(ageGroup) }
        var isUploadMenuOpen by remember { mutableStateOf(false) }
        var isUploading by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { if (!isUploading) showUploadDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBgColor, RoundedCornerShape(20.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconLabel("Upload New Story (Admin)", Icons.Filled.CloudUpload, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pageTextColor)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Assign to Grade Level:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(6.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { isUploadMenuOpen = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Category: ${uploadTargetAgeGroup.title}", fontWeight = FontWeight.SemiBold, color = pageTextColor)
                        }
                        DropdownMenu(
                            expanded = isUploadMenuOpen,
                            onDismissRequest = { isUploadMenuOpen = false }
                        ) {
                            AgeGroup.values().forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.title) },
                                    onClick = {
                                        uploadTargetAgeGroup = g
                                        isUploadMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = jsonText,
                        onValueChange = { jsonText = it },
                        placeholder = { Text("Paste JSON story template here...", fontSize = 12.sp, color = Color.Gray) },
                        colors = themedFieldColors(pageTextColor),
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                        maxLines = 8
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (jsonText.isNotBlank()) {
                                isUploading = true
                                scope.launch {
                                    val success = StorySeeder.uploadStoryFromJson(jsonText, uploadTargetAgeGroup, context)
                                    isUploading = false
                                    if (success) {
                                        showUploadDialog = false
                                        onRefreshStories()
                                    }
                                }
                            }
                        },
                        enabled = !isUploading && jsonText.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                        } else {
                            IconLabel("Upload to Firestore", Icons.Filled.CloudUpload, iconAtEnd = true, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    TextButton(onClick = { showUploadDialog = false }, enabled = !isUploading) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            }
        }
    }
}
