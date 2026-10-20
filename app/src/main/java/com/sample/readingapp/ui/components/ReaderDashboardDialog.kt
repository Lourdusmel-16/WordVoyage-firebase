package com.sample.readingapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sample.readingapp.data.*
import com.sample.readingapp.theme.*

// ---------------- READER DASHBOARD DIALOG (PROFILE & GRADE + READ STORIES) ----------------
@Composable
fun ReaderDashboardDialog(
    username: String,
    ageGroup: AgeGroup,
    stories: List<Story>,
    playedStoryIds: List<String>,
    completedStoriesMap: Map<String, CompletedStoryRecord>,
    pageBgColor: Color,
    pageTextColor: Color,
    isResetPending: Boolean,
    onClose: () -> Unit,
    onSave: (String, AgeGroup) -> Unit,
    onRequestReset: () -> Unit,
    onSelectStory: (Story) -> Unit,
    onLogout: () -> Unit
) {
    var tempName by remember { mutableStateOf(username) }
    var tempGroup by remember { mutableStateOf(ageGroup) }
    var isAgeMenuOpen by remember { mutableStateOf(false) }
    var selectedProfileTab by remember { mutableStateOf(0) }
    var showLogoutConfirmInProfile by remember { mutableStateOf(false) }

    val allCompletedStories = remember(completedStoriesMap.size, stories, playedStoryIds.size) {
        val list = mutableListOf<CompletedStoryRecord>()
        playedStoryIds.forEach { id ->
            val record = completedStoriesMap[id] ?: run {
                val s = stories.firstOrNull { it.id == id }
                if (s != null) CompletedStoryRecord(s.id, s.title, s.gradeLevel, s.screens.size)
                else null
            }
            if (record != null && list.none { it.id == record.id }) {
                list.add(record)
            }
        }
        list.sortedByDescending { it.completedAt }
    }

    if (showLogoutConfirmInProfile) {
        Dialog(onDismissRequest = { showLogoutConfirmInProfile = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(pageBgColor, RoundedCornerShape(18.dp))
                    .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(18.dp))
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Log Out?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pageTextColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Are you sure you want to log out of your reader account?", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showLogoutConfirmInProfile = false },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("Cancel", color = pageTextColor, fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = {
                                showLogoutConfirmInProfile = false
                                onClose()
                                onLogout()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("Yes, Log Out", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(pageBgColor, RoundedCornerShape(20.dp))
                .border(1.dp, BrandShadeBorder, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconLabel("Reader Dashboard", Icons.Filled.Person, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pageTextColor)
                Spacer(modifier = Modifier.height(12.dp))

                TabRow(
                    selectedTabIndex = selectedProfileTab,
                    containerColor = Color.Transparent,
                    contentColor = BrandShadeBold
                ) {
                    Tab(
                        selected = selectedProfileTab == 0,
                        unselectedContentColor = pageTextColor.copy(alpha = 0.7f),
                        onClick = { selectedProfileTab = 0 },
                        text = { Text("Profile & Grade", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedProfileTab == 1,
                        unselectedContentColor = pageTextColor.copy(alpha = 0.7f),
                        onClick = { selectedProfileTab = 1 },
                        text = { Text("Read Stories (${allCompletedStories.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedProfileTab == 0) {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Reader's Username") },
                        singleLine = true,
                        colors = themedFieldColors(pageTextColor),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = tempGroup.title,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Grade Level") },
                            colors = themedFieldColors(pageTextColor),
                            trailingIcon = {
                                IconButton(onClick = { isAgeMenuOpen = true }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = pageTextColor)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { isAgeMenuOpen = true }
                        )

                        DropdownMenu(
                            expanded = isAgeMenuOpen,
                            onDismissRequest = { isAgeMenuOpen = false }
                        ) {
                            AgeGroup.values().forEach { g ->
                                DropdownMenuItem(
                                    text = { Text(g.title) },
                                    onClick = {
                                        tempGroup = g
                                        isAgeMenuOpen = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // BUTTON TEXT CHANGES TO "SAVE CHANGES & RESET" AFTER THE USER CONFIRMS YES ON RESET
                    Button(
                        onClick = {
                            if (tempName.isNotBlank()) {
                                onSave(tempName.trim(), tempGroup)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isResetPending) Color(0xFFDC2626) else BrandShadeBold
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text(
                            text = if (isResetPending) "Save Changes & Reset" else "Save & Apply Changes",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // RESET PROGRESS BUTTON IN PROFILE DASHBOARD (YES/NO FIRST, THEN SAVE CHANGES & RESET)
                    OutlinedButton(
                        onClick = {
                            onRequestReset()
                        },
                        border = BorderStroke(1.dp, BrandShadeMedium),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandShadeBold),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        IconLabel("Reset Reading Progress", Icons.Filled.Refresh, fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            showLogoutConfirmInProfile = true
                        },
                        border = BorderStroke(1.dp, Color(0xFFEF4444)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(42.dp)
                    ) {
                        IconLabel("Log Out", Icons.Filled.ExitToApp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    if (allCompletedStories.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No stories completed yet! Finish all pages of a story to add it here",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            items(allCompletedStories) { readStory ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable {
                                            val target = stories.firstOrNull { it.id == readStory.id }
                                            if (target != null) {
                                                onClose()
                                                onSelectStory(target)
                                            }
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = BrandShadeSoft),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = readStory.title,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandShadeDeep
                                            )
                                            Text(
                                                text = "${readStory.gradeLevel} • ${readStory.totalPages} / ${readStory.totalPages} Pages Read",
                                                fontSize = 12.sp,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onClose) {
                    Text("Close", color = Color.Gray)
                }
            }
        }
    }
}
