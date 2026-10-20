package com.sample.readingapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StoryRepositoryTest {

    @Test
    fun parseStories_readsAnArrayAndKeepsIds() {
        val json = """[{"id":"a1","title":"One","ageGroup":"GRADES_1_3","screens":["Hello there"]}]"""
        val stories = StoryRepository.parseStories(json)
        assertEquals(1, stories.size)
        assertEquals("a1", stories[0].id)
        assertEquals("GRADES_1_3", stories[0].ageGroup)
    }

    @Test
    fun parseStories_acceptsStoriesObject() {
        val json = """{"stories":[{"id":"b2","title":"Two","screens":["Page one"]}]}"""
        assertEquals(1, StoryRepository.parseStories(json).size)
    }

    @Test
    fun parseStories_makesAnIdWhenMissing_andSkipsInvalidStories() {
        val json = """[{"title":"My Story","gradeLevel":"Grades 6 to 8","screens":["x"]},{"title":"","screens":[]}]"""
        val stories = StoryRepository.parseStories(json)
        assertEquals(1, stories.size)
        assertEquals("story_my_story", stories[0].id)
        assertEquals("GRADES_6_8", stories[0].ageGroup)
        assertTrue(stories[0].screens.isNotEmpty())
    }
}
