package com.example.data

import kotlinx.coroutines.flow.Flow

class MockExamRepository(private val mockExamDao: MockExamDao) {
    val allExams: Flow<List<MockExam>> = mockExamDao.getAllExams()
    val allAttempts: Flow<List<TestAttempt>> = mockExamDao.getAllAttempts()

    suspend fun getExamById(id: String): MockExam? {
        return mockExamDao.getExamById(id)
    }

    suspend fun insertExam(exam: MockExam) {
        mockExamDao.insertExam(exam)
    }

    suspend fun deleteExam(id: String) {
        mockExamDao.deleteExamById(id)
    }

    suspend fun insertAttempt(attempt: TestAttempt) {
        mockExamDao.insertAttempt(attempt)
    }

    suspend fun deleteAttempt(id: Int) {
        mockExamDao.deleteAttemptById(id)
    }
}
