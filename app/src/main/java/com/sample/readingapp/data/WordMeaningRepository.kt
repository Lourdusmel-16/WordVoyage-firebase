package com.sample.readingapp.data

import com.sample.readingapp.util.cleanWord
import com.sample.readingapp.util.getWordStem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

// Looks up the meaning of a word using the free dictionaryapi.dev service (needs internet).
object WordMeaningRepository {

    data class Definition(val partOfSpeech: String, val text: String, val example: String?)

    sealed class Result {
        data class Found(val phonetic: String?, val definitions: List<Definition>) : Result()
        object NotFound : Result()
        object NetworkError : Result()
    }

    suspend fun lookup(word: String): Result = withContext(Dispatchers.IO) {
        val clean = cleanWord(word)
        if (clean.isBlank()) return@withContext Result.NotFound

        // Try the word itself first, then its stem (e.g. "jumped" -> "jump")
        for (candidate in listOf(clean, getWordStem(clean)).distinct()) {
            val r = request(candidate)
            if (r is Result.Found || r is Result.NetworkError) return@withContext r
        }
        Result.NotFound
    }

    private fun request(word: String): Result {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL("https://api.dictionaryapi.dev/api/v2/entries/en/" + URLEncoder.encode(word, "UTF-8"))
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            when (connection.responseCode) {
                200 -> parse(connection.inputStream.bufferedReader().use { it.readText() })
                404 -> Result.NotFound
                else -> Result.NetworkError
            }
        } catch (e: Exception) {
            Result.NetworkError
        } finally {
            connection?.disconnect()
        }
    }

    private fun clean(s: String?): String? = s?.trim()?.takeIf { it.isNotEmpty() && it != "null" }

    private fun parse(body: String): Result {
        return try {
            val root = JSONArray(body)
            if (root.length() == 0) return Result.NotFound
            val entry = root.getJSONObject(0)
            val phonetic = clean(entry.optString("phonetic"))
            val definitions = mutableListOf<Definition>()
            val meanings = entry.optJSONArray("meanings")
            if (meanings != null) {
                for (i in 0 until meanings.length()) {
                    val meaning = meanings.getJSONObject(i)
                    val part = meaning.optString("partOfSpeech")
                    val defs = meaning.optJSONArray("definitions") ?: continue
                    for (j in 0 until minOf(defs.length(), 2)) {
                        val d = defs.getJSONObject(j)
                        val text = clean(d.optString("definition")) ?: continue
                        definitions.add(Definition(part, text, clean(d.optString("example"))))
                        if (definitions.size >= 4) break
                    }
                    if (definitions.size >= 4) break
                }
            }
            if (definitions.isEmpty()) Result.NotFound else Result.Found(phonetic, definitions)
        } catch (e: Exception) {
            Result.NotFound
        }
    }
}
