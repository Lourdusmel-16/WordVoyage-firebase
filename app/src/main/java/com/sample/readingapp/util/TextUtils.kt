package com.sample.readingapp.util

import kotlin.math.min

fun cleanWord(w: String): String = w.lowercase().filter { it.isLetterOrDigit() }
fun consonantSkeleton(w: String): String = cleanWord(w).filter { it !in "aeiouy" }

// KEY USED TO STORE THE LAST WORD REACHED ON EVERY PAGE OF EVERY STORY (FIRESTORE MAP KEY)
fun pageWordKey(storyId: String, pageIndex: Int): String = "${storyId}__p$pageIndex"

fun getPhoneticHint(word: String): String {
    val clean = cleanWord(word)
    if (clean.length <= 3) return clean

    val vowels = "aeiouy"
    val result = StringBuilder()
    var i = 0

    while (i < clean.length) {
        result.append(clean[i])
        if (i < clean.length - 2 && clean[i] in vowels && clean[i + 1] !in vowels && clean[i + 2] !in vowels) {
            result.append("-")
            result.append(clean[i + 1])
            i++
        } else if (i < clean.length - 2 && clean[i] in vowels && clean[i + 1] !in vowels && clean[i + 2] in vowels) {
            result.append("-")
        }
        i++
    }
    return result.toString()
}

fun getWordStem(word: String): String {
    var clean = cleanWord(word)
    val suffixes = listOf("ing", "tion", "ed", "es", "ly", "er", "est", "s")
    for (suffix in suffixes) {
        if (clean.endsWith(suffix) && clean.length - suffix.length >= 3) {
            clean = clean.removeSuffix(suffix)
            break
        }
    }
    return clean
}

fun levenshteinDistance(s1: String, s2: String): Int {
    val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
    for (i in 0..s1.length) dp[i][0] = i
    for (j in 0..s2.length) dp[0][j] = j
    for (i in 1..s1.length) {
        for (j in 1..s2.length) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = min(min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost)
        }
    }
    return dp[s1.length][s2.length]
}

fun isSimilarWord(spoken: String, target: String): Boolean {
    val s = cleanWord(spoken)
    val t = cleanWord(target)
    if (s.isEmpty() || t.isEmpty()) return false
    if (s == t) return true
    if (getWordStem(s) == getWordStem(t)) return true

    val sk1 = consonantSkeleton(s)
    val sk2 = consonantSkeleton(t)
    if (sk1.isNotEmpty() && sk1 == sk2) return true

    if (s.length >= 3 && t.startsWith(s)) return true
    if (t.length >= 3 && s.startsWith(t)) return true

    val maxAllowed = if (t.length >= 5) 2 else 1
    return levenshteinDistance(s, t) <= maxAllowed
}
