package com.sample.readingapp.data



data class Story(
    val id: String = "",
    val title: String = "",
    val gradeLevel: String = "Grades 4 to 5",
    val ageGroup: String = AgeGroup.GRADES_4_5.name,
    val difficulty: String = "Medium",
    val screens: List<String> = emptyList(),
    val quiz: Quiz = Quiz(),
    val order: Int = 0
)

data class Quiz(
    val question: String = "",
    val options: List<String> = emptyList(),
    val correctIndex: Int = 0
)
