package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a parsed Question in the Mock Test.
 */
data class Question(
    val id: String,
    val questionText: String,
    val options: List<String>,       // e.g. ["A) Option 1", "B) Option 2", ...]
    val correctAnswer: String,     // e.g., "A", "B", "C", "D"
    val explanation: String,
    val category: String = "General",
    val difficulty: String = "Medium"
)

/**
 * Room Entity representing a completed Mock Exam paper.
 */
@Entity(tableName = "mock_exams")
data class MockExam(
    @PrimaryKey val id: String,
    val title: String,
    val category: String,
    val timeLimitMinutes: Int,
    val questionsJson: String,      // JSON serialized string of List<Question>
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Room Entity representing a user's attempt at an exam.
 */
@Entity(tableName = "test_attempts")
data class TestAttempt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val examId: String,
    val examTitle: String,
    val examCategory: String,
    val score: Int,
    val totalQuestions: Int,
    val timeTakenSeconds: Long,
    val answersJson: String,        // JSON serialized string of Map<Int, String> (Question Index -> Selected Answer)
    val scorePercentage: Float,
    val timestamp: Long = System.currentTimeMillis()
)
