package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ParsedExam(
    val title: String,
    val category: String,
    val timeLimitMinutes: Int,
    val questions: List<ParsedQuestion>
)

@JsonClass(generateAdapter = true)
data class ParsedQuestion(
    val questionText: String,
    val options: List<String>,       // e.g. ["A) Mechanics", "B) Optics", ...]
    val correctAnswer: String,     // e.g. "A", "B", "C", "D"
    val explanation: String,
    val category: String = "General",
    val difficulty: String = "Medium"
)
