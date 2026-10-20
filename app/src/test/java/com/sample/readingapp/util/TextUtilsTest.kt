package com.sample.readingapp.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TextUtilsTest {

    @Test
    fun cleanWord_removesPunctuationAndLowercases() {
        assertEquals("hello", cleanWord("Hello,"))
        assertEquals("its", cleanWord("It's"))
    }

    @Test
    fun consonantSkeleton_dropsVowels() {
        assertEquals("rd", consonantSkeleton("Read"))
    }

    @Test
    fun pageWordKey_buildsFirestoreKey() {
        assertEquals("story1__p2", pageWordKey("story1", 2))
    }

    @Test
    fun levenshteinDistance_knownValues() {
        assertEquals(3, levenshteinDistance("kitten", "sitting"))
        assertEquals(3, levenshteinDistance("", "abc"))
        assertEquals(0, levenshteinDistance("same", "same"))
    }

    @Test
    fun getWordStem_stripsCommonSuffixes() {
        assertEquals("jump", getWordStem("jumped"))
        assertEquals("cat", getWordStem("cats"))
        assertEquals("it", getWordStem("it"))
    }

    @Test
    fun getPhoneticHint_shortWordsAreUnchanged() {
        assertEquals("cat", getPhoneticHint("cat"))
    }

    @Test
    fun getPhoneticHint_longWordsAreSplitWithHyphens() {
        assertEquals("ra-bbit", getPhoneticHint("rabbit"))
    }

    @Test
    fun isSimilarWord_acceptsExactAndPunctuatedMatches() {
        assertTrue(isSimilarWord("cat", "cat"))
        assertTrue(isSimilarWord("Hello!", "hello"))
    }

    @Test
    fun isSimilarWord_acceptsSameStemOrPrefix() {
        assertTrue(isSimilarWord("running", "run"))
        assertTrue(isSimilarWord("jumpd", "jumped"))
    }

    @Test
    fun isSimilarWord_rejectsDifferentWordsAndBlanks() {
        assertFalse(isSimilarWord("dog", "elephant"))
        assertFalse(isSimilarWord("the", "a"))
        assertFalse(isSimilarWord("", "cat"))
    }
}
