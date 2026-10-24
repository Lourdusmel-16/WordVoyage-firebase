package com.sample.readingapp.data

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

object StoryRepository {
    private val db get() = FirebaseFirestore.getInstance()

    fun parseStories(json: String): List<Story> {
        return try {
            val gson = Gson()
            val trimmed = json.trim()
            val rawStories: List<Story> = if (trimmed.startsWith("{")) {
                val container = gson.fromJson(trimmed, StoriesContainer::class.java)
                container?.stories ?: emptyList()
            } else {
                val arrayType = object : com.google.gson.reflect.TypeToken<List<Story>>() {}.type
                gson.fromJson(trimmed, arrayType) ?: emptyList()
            }

            rawStories.mapNotNull { story ->
                if (story.title.isBlank() || story.screens.isEmpty()) {
                    null
                } else {
                    val id = if (story.id.isBlank()) {
                        "story_" + story.title.lowercase().replace(Regex("[^a-z0-9]"), "_").replace(Regex("_+"), "_").trim('_')
                    } else {
                        story.id
                    }
                    val ageGroup = if (story.ageGroup.isBlank() || (story.ageGroup == AgeGroup.GRADES_4_5.name && story.gradeLevel != "Grades 4 to 5")) {
                        AgeGroup.fromString(story.gradeLevel).name
                    } else if (story.ageGroup.isBlank()) {
                        AgeGroup.GRADES_4_5.name
                    } else {
                        story.ageGroup
                    }
                    story.copy(id = id, ageGroup = ageGroup)
                }
            }
        } catch (e: Exception) {
            Log.e("StoryRepository", "Error parsing stories: ${e.message}")
            emptyList()
        }
    }

    private data class StoriesContainer(
        val stories: List<Story> = emptyList()
    )

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
