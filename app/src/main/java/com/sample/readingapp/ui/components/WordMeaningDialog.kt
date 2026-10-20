package com.sample.readingapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sample.readingapp.data.WordMeaningRepository

// ---------------- WORD MEANING DIALOG (OPENED BY LONG-PRESSING A WORD) ----------------
@Composable
fun WordMeaningDialog(
    word: String,
    pageBgColor: Color,
    pageTextColor: Color,
    highlightColor: Color,
    fontFamily: FontFamily,
    onDismiss: () -> Unit
) {
    var result by remember(word) { mutableStateOf<WordMeaningRepository.Result?>(null) }
    LaunchedEffect(word) {
        result = WordMeaningRepository.lookup(word)
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(pageBgColor, RoundedCornerShape(18.dp))
                .border(1.5.dp, highlightColor, RoundedCornerShape(18.dp))
                .padding(22.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconLabel(
                    word,
                    Icons.Filled.MenuBook,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = fontFamily,
                    color = pageTextColor
                )
                Spacer(modifier = Modifier.height(10.dp))

                when (val r = result) {
                    null -> {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp, color = highlightColor)
                    }
                    is WordMeaningRepository.Result.Found -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!r.phonetic.isNullOrBlank()) {
                                Text(r.phonetic, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = highlightColor)
                            }
                            r.definitions.forEach { d ->
                                Column {
                                    if (d.partOfSpeech.isNotBlank()) {
                                        Text(d.partOfSpeech, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                    }
                                    Text(d.text, fontSize = 14.sp, color = pageTextColor)
                                    if (!d.example.isNullOrBlank()) {
                                        Text("\"${d.example}\"", fontSize = 12.sp, color = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                    WordMeaningRepository.Result.NotFound -> {
                        Text("Sorry, no meaning was found for this word.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                    WordMeaningRepository.Result.NetworkError -> {
                        Text("Could not load the meaning. Please check your internet connection and try again.", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color.Gray)
                }
            }
        }
    }
}
