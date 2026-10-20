package com.sample.readingapp.data



data class CompletedStoryRecord(
    val id: String,
    val title: String,
    val gradeLevel: String,
    val totalPages: Int,
    val completedAt: Long = System.currentTimeMillis()
)
