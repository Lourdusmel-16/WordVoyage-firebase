package com.sample.readingapp.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object StoryRepository {
    private val db get() = FirebaseFirestore.getInstance()

    /** Strictly retrieves stories belonging ONLY to the user's AgeGroup */
    suspend fun fetchStoriesByAgeGroup(ageGroup: AgeGroup): List<Story> {
        return try {
            val querySnap = db.collection("stories")
                .whereEqualTo("ageGroup", ageGroup.name)
                .get()
                .await()

            val groupStories = querySnap.documents.mapNotNull { doc ->
                val story = doc.toObject(Story::class.java)?.copy(id = doc.id)
                if (story != null && story.title.isNotBlank() && story.screens.isNotEmpty()) {
                    story
                } else null
            }

            if (groupStories.isNotEmpty()) {
                groupStories.sortedBy { it.order }
            } else {
                // Secondary check: look for documents explicitly matching the grade level badge
                fetchAllStories().filter { story ->
                    story.ageGroup.equals(ageGroup.name, ignoreCase = true) ||
                            story.gradeLevel.contains(ageGroup.badge, ignoreCase = true) ||
                            story.gradeLevel.contains(ageGroup.title, ignoreCase = true)
                }.sortedBy { it.order }
            }
        } catch (e: Exception) {
            Log.e("StoryRepository", "Error fetching stories by age group: ${e.message}")
            emptyList()
        }
    }

    private suspend fun fetchAllStories(): List<Story> {
        return try {
            val snap = db.collection("stories").get().await()
            snap.documents
                .mapNotNull { doc ->
                    val story = doc.toObject(Story::class.java)?.copy(id = doc.id)
                    if (story != null && story.title.isNotBlank() && story.screens.isNotEmpty()) {
                        story
                    } else null
                }
                .sortedBy { it.order }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
