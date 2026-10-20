package com.sample.readingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

// ---------------- YES / NO RESET CONFIRMATION DIALOG ----------------
@Composable
fun ResetYesNoDialog(
    pageBgColor: Color,
    pageTextColor: Color,
    onYes: () -> Unit,
    onNo: () -> Unit
) {
    Dialog(onDismissRequest = onNo) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(pageBgColor, RoundedCornerShape(18.dp))
                .border(1.5.dp, BrandShadeBorder, RoundedCornerShape(18.dp))
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconLabel("Reset Progress?", Icons.Filled.Refresh, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pageTextColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Yes or no, do you want to reset progress?",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onNo,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("No", color = pageTextColor, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }
                    Button(
                        onClick = onYes,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).defaultMinSize(minHeight = 44.dp)
                    ) {
                        Text("Yes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                    }
                }
            }
        }
    }
}

// ---------------- RESET SCOPE DIALOG (CURRENT GRADE OR EVERY GRADE) ----------------
@Composable
fun ResetScopeDialog(
    ageGroup: AgeGroup,
    pageBgColor: Color,
    pageTextColor: Color,
    onDismiss: () -> Unit,
    onResetCurrentGrade: () -> Unit,
    onResetAllGrades: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(pageBgColor, RoundedCornerShape(20.dp))
                .border(1.5.dp, BrandShadeBorder, RoundedCornerShape(20.dp))
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconLabel("Reset Reading Progress", Icons.Filled.Refresh, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = pageTextColor)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Would you like to reset progress for ${ageGroup.title} only, or reset all reading progress across every grade level?",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onResetCurrentGrade,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandShadeBold),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Reset ${ageGroup.badge} Only (${ageGroup.title})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onResetAllGrades,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp)
                ) {
                    Text("Reset All Grades (Full Reset)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onCancel,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 42.dp)
                ) {
                    Text("Cancel", color = pageTextColor, fontSize = 13.sp, maxLines = 1, softWrap = false)
                }
            }
        }
    }
}
