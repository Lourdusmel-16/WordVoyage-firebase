package com.sample.readingapp.data



enum class AgeGroup(val title: String, val badge: String) {
    GRADES_1_3("Grades 1 to 3", "G1-3"),
    GRADES_4_5("Grades 4 to 5", "G4-5"),
    GRADES_6_8("Grades 6 to 8", "G6-8"),
    GRADES_9_12("Grades 9 to 12", "G9-12");

    companion object {
        fun fromString(value: String?): AgeGroup {
            return values().firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                        it.title.equals(value, ignoreCase = true) ||
                        it.badge.equals(value, ignoreCase = true)
            } ?: GRADES_4_5
        }
    }
}
