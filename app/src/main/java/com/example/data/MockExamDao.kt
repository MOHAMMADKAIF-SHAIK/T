package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MockExamDao {

    @Query("SELECT * FROM mock_exams ORDER BY timestamp DESC")
    fun getAllExams(): Flow<List<MockExam>>

    @Query("SELECT * FROM mock_exams WHERE id = :id LIMIT 1")
    suspend fun getExamById(id: String): MockExam?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExam(exam: MockExam)

    @Query("DELETE FROM mock_exams WHERE id = :id")
    suspend fun deleteExamById(id: String)

    @Query("SELECT * FROM test_attempts ORDER BY timestamp DESC")
    fun getAllAttempts(): Flow<List<TestAttempt>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: TestAttempt)

    @Query("DELETE FROM test_attempts WHERE id = :id")
    suspend fun deleteAttemptById(id: Int)
}
