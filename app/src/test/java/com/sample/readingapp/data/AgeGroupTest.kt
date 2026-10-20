package com.sample.readingapp.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AgeGroupTest {

    @Test
    fun fromString_matchesNameTitleAndBadge() {
        assertEquals(AgeGroup.GRADES_1_3, AgeGroup.fromString("GRADES_1_3"))
        assertEquals(AgeGroup.GRADES_6_8, AgeGroup.fromString("grades 6 to 8"))
        assertEquals(AgeGroup.GRADES_9_12, AgeGroup.fromString("G9-12"))
    }

    @Test
    fun fromString_fallsBackToGrades4to5() {
        assertEquals(AgeGroup.GRADES_4_5, AgeGroup.fromString(null))
        assertEquals(AgeGroup.GRADES_4_5, AgeGroup.fromString("unknown"))
    }
}
