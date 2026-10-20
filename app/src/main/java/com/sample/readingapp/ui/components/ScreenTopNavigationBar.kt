package com.sample.readingapp.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sample.readingapp.data.*
import com.sample.readingapp.theme.*
import com.sample.readingapp.ui.reader.*

// ---------------- SCREEN-LEVEL TOP NAVIGATION BAR (WITH RESET PROGRESS OPTION) ----------------
@Composable
fun ScreenTopNavigationBar(
    username: String,
    ageGroup: AgeGroup,
    isAdmin: Boolean,
    readerTheme: ReaderTheme,
    pageTextColor: Color,
    onProfileClick: () -> Unit,
    onResetProgressClick: () -> Unit,
    onThemeChanged: (ReaderTheme) -> Unit,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    var showThemeMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showExitAppConfirmDialog by remember { mutableStateOf(false) }

    if (showLogoutConfirmDialog) {
        Dialog(onDismissRequest = { showLogoutConfirmDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandShadeSoft, RoundedCornerShape(18.dp))
                    .border(1.5.dp, BrandShadeBorder, RoundedCornerShape(18.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Log Out?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandShadeDeep)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Are you sure you want to log out of your reader account?", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showLogoutConfirmDialog = false },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("Cancel", color = BrandShadeDeep, fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = {
                                showLogoutConfirmDialog = false
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

    if (showExitAppConfirmDialog) {
        Dialog(onDismissRequest = { showExitAppConfirmDialog = false }) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandShadeSoft, RoundedCornerShape(18.dp))
                    .border(1.5.dp, BrandShadeBorder, RoundedCornerShape(18.dp))
                    .padding(22.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Exit App?", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BrandShadeDeep)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Are you sure you want to exit the app now?", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { showExitAppConfirmDialog = false },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("Cancel", color = BrandShadeDeep, fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                        Button(
                            onClick = {
                                showExitAppConfirmDialog = false
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onProfileClick() }
                .background(BrandShadeSoft, RoundedCornerShape(18.dp))
                .border(1.dp, BrandShadeBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(BrandShadeBold, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(username.take(1).uppercase(), color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(username, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandShadeDeep)
                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Filled.Star, contentDescription = "Admin", tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                    }
                }
                Text(ageGroup.badge, fontSize = 12.sp, color = BrandShadeMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box {
                OutlinedButton(
                    onClick = { showThemeMenu = true },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color.Gray.copy(alpha = 0.35f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    IconLabel(readerTheme.title, readerTheme.icon, fontSize = 12.sp, color = pageTextColor)
                }

                DropdownMenu(
                    expanded = showThemeMenu,
                    onDismissRequest = { showThemeMenu = false }
                ) {
                    ReaderTheme.values().forEach { theme ->
                        DropdownMenuItem(
                            text = { IconLabel(theme.title, theme.icon) },
                            onClick = {
                                onThemeChanged(theme)
                                showThemeMenu = false
                            }
                        )
                    }
                    MatchDeviceMenuItem(onThemeChanged = onThemeChanged, onDone = { showThemeMenu = false })
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Box {
                IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Settings", tint = pageTextColor)
                }

                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { IconLabel("Reader Dashboard", Icons.Filled.Person) },
                        onClick = {
                            showMoreMenu = false
                            onProfileClick()
                        }
                    )
                    // RESET PROGRESS IN SETTINGS MENU
                    DropdownMenuItem(
                        text = { IconLabel("Reset Progress", Icons.Filled.Refresh, fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            showMoreMenu = false
                            onResetProgressClick()
                        }
                    )
                    DropdownMenuItem(
                        text = { IconLabel("Re-explain Stories", Icons.Filled.Explore, fontWeight = FontWeight.SemiBold) },
                        onClick = {
                            showMoreMenu = false
                            GuideSession.onReplay?.invoke()
                        }
                    )
                    Divider()
                    DropdownMenuItem(
                        text = { IconLabel("Logout", Icons.Filled.ExitToApp, color = Color(0xFFDC2626), fontWeight = FontWeight.Bold) },
                        onClick = {
                            showMoreMenu = false
                            showLogoutConfirmDialog = true
                        }
                    )
                    DropdownMenuItem(
                        text = { IconLabel("Exit App", Icons.Filled.Close, color = Color(0xFFDC2626)) },
                        onClick = {
                            showMoreMenu = false
                            showExitAppConfirmDialog = true
                        }
                    )
                }
            }
        }
    }
}
