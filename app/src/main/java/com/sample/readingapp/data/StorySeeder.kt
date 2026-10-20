package com.sample.readingapp.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

object StorySeeder {

    suspend fun uploadSingleStory(story: Story, targetAgeGroup: AgeGroup? = null, context: Context? = null): Boolean {
        return try {
            val db = FirebaseFirestore.getInstance()
            val docId = story.id.ifBlank { "story_${System.currentTimeMillis()}" }

            val effectiveAgeGroup = targetAgeGroup ?: AgeGroup.fromString(story.ageGroup.ifBlank { story.gradeLevel })
            val finalizedStory = story.copy(
                id = docId,
                ageGroup = effectiveAgeGroup.name,
                gradeLevel = if (story.gradeLevel.isNotBlank()) story.gradeLevel else effectiveAgeGroup.title
            )

            db.collection("stories").document(docId).set(finalizedStory, SetOptions.merge()).await()
            showToast(context, "Story '${finalizedStory.title}' uploaded to ${effectiveAgeGroup.title}!")
            true
        } catch (e: Exception) {
            Log.e("StorySeeder", "Error uploading story: ${e.message}", e)
            showToast(context, "Upload Failed: ${e.localizedMessage}")
            false
        }
    }

    suspend fun uploadStoryFromJson(
        jsonStr: String,
        targetAgeGroup: AgeGroup? = null,
        context: Context? = null
    ): Boolean {
        return try {
            val gson = Gson()
            val story = gson.fromJson(jsonStr.trim(), Story::class.java)
            if (story.title.isBlank() || story.screens.isEmpty()) {
                showToast(context, "Error: Story must have a title and screens!")
                return false
            }
            uploadSingleStory(story, targetAgeGroup, context)
        } catch (e: Exception) {
            Log.e("StorySeeder", "JSON Parse error: ${e.message}", e)
            showToast(context, "Invalid JSON format: ${e.localizedMessage}")
            false
        }
    }

    private fun showToast(context: Context?, msg: String) {
        if (context == null) return
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }
}
