package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

object MoshiUtils {
    val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun serializeQuestions(questions: List<Question>): String {
        val type = Types.newParameterizedType(List::class.java, Question::class.java)
        val adapter = moshi.adapter<List<Question>>(type)
        return adapter.toJson(questions)
    }

    fun deserializeQuestions(json: String): List<Question> {
        return try {
            val type = Types.newParameterizedType(List::class.java, Question::class.java)
            val adapter = moshi.adapter<List<Question>>(type)
            adapter.fromJson(json) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun serializeAnswers(answers: Map<Int, String>): String {
        val type = Types.newParameterizedType(Map::class.java, java.lang.Integer::class.java, String::class.java)
        val adapter = moshi.adapter<Map<Int, String>>(type)
        return adapter.toJson(answers)
    }

    fun deserializeAnswers(json: String): Map<Int, String> {
        return try {
            val type = Types.newParameterizedType(Map::class.java, java.lang.Integer::class.java, String::class.java)
            val adapter = moshi.adapter<Map<Int, String>>(type)
            adapter.fromJson(json) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    fun serializeParsedExam(exam: ParsedExam): String {
        val adapter = moshi.adapter(ParsedExam::class.java)
        return adapter.toJson(exam)
    }

    fun deserializeParsedExam(json: String): ParsedExam? {
        return try {
            val adapter = moshi.adapter(ParsedExam::class.java)
            adapter.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }
}
