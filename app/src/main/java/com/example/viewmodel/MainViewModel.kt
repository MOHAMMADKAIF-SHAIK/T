package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiApiClient
import com.example.api.GeminiContent
import com.example.api.GeminiGenerationConfig
import com.example.api.GeminiInlineData
import com.example.api.GeminiPart
import com.example.api.GeminiRequest
import com.example.data.AppDatabase
import com.example.data.MockExam
import com.example.data.MockExamRepository
import com.example.data.MoshiUtils
import com.example.data.ParsedExam
import com.example.data.Question
import com.example.data.SampleExams
import com.example.data.TestAttempt
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

enum class AppScreen {
    HOME,
    LOADING_EXAM,
    TEST_SESSION,
    ANALYTICS,
    ATTEMPT_DETAILS
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MockExamRepository
    
    // UI Screen navigation
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Database Flows
    val allExams: StateFlow<List<MockExam>>
    val allAttempts: StateFlow<List<TestAttempt>>

    // Parser State
    private val _isParsing = MutableStateFlow(false)
    val isParsing: StateFlow<Boolean> = _isParsing.asStateFlow()

    private val _parsingError = MutableStateFlow<String?>(null)
    val parsingError: StateFlow<String?> = _parsingError.asStateFlow()

    // Active Test State
    private val _activeExam = MutableStateFlow<MockExam?>(null)
    val activeExam: StateFlow<MockExam?> = _activeExam.asStateFlow()

    private val _activeQuestions = MutableStateFlow<List<Question>>(emptyList())
    val activeQuestions: StateFlow<List<Question>> = _activeQuestions.asStateFlow()

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    // Selected answers: Index of question -> selected option letter ("A", "B", "C", "D")
    private val _userAnswers = MutableStateFlow<Map<Int, String>>(emptyMap())
    val userAnswers: StateFlow<Map<Int, String>> = _userAnswers.asStateFlow()

    // Bookmarked/Review Questions
    private val _bookmarkedQuestions = MutableStateFlow<Set<Int>>(emptySet())
    val bookmarkedQuestions: StateFlow<Set<Int>> = _bookmarkedQuestions.asStateFlow()

    // Timer State
    private val _timeRemainingSeconds = MutableStateFlow(0)
    val timeRemainingSeconds: StateFlow<Int> = _timeRemainingSeconds.asStateFlow()

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning.asStateFlow()

    private var examDurationSeconds = 0
    private var timerJob: Job? = null

    // Completed Analytics View (or detail view)
    private val _selectedAttempt = MutableStateFlow<TestAttempt?>(null)
    val selectedAttempt: StateFlow<TestAttempt?> = _selectedAttempt.asStateFlow()

    private val _selectedAttemptExam = MutableStateFlow<MockExam?>(null)
    val selectedAttemptExam: StateFlow<MockExam?> = _selectedAttemptExam.asStateFlow()

    // AI Coach Chat State
    private val _aiCoachLoading = MutableStateFlow(false)
    val aiCoachLoading: StateFlow<Boolean> = _aiCoachLoading.asStateFlow()

    private val _aiCoachResponse = MutableStateFlow<String?>(null)
    val aiCoachResponse: StateFlow<String?> = _aiCoachResponse.asStateFlow()

    private val _currentCoachQuestionId = MutableStateFlow<String?>(null)
    val currentCoachQuestionId: StateFlow<String?> = _currentCoachQuestionId.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = MockExamRepository(database.mockExamDao())

        allExams = repository.allExams.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        allAttempts = repository.allAttempts.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Seed sample data if database is empty
        viewModelScope.launch {
            val exams = repository.allExams.first()
            if (exams.isEmpty()) {
                SampleExams.examsList.forEach { sampleExam ->
                    repository.insertExam(sampleExam)
                }
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.HOME) {
            stopTimer()
            _aiCoachResponse.value = null
            _currentCoachQuestionId.value = null
        }
    }

    // --- PDF / Text Parsing using Gemini API ---
    fun createMockTestFromPdf(context: Context, uri: Uri) {
        _isParsing.value = true
        _parsingError.value = null
        _currentScreen.value = AppScreen.LOADING_EXAM

        viewModelScope.launch {
            try {
                val pdfBytes = withContext(Dispatchers.IO) {
                    val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
                    val bytes = inputStream?.readBytes()
                    inputStream?.close()
                    bytes
                }

                if (pdfBytes == null) {
                    _parsingError.value = "Failed to read PDF file. Please try again."
                    _isParsing.value = false
                    return@launch
                }

                val base64Pdf = Base64.encodeToString(pdfBytes, Base64.NO_WRAP)
                val inlineData = GeminiInlineData(mimeType = "application/pdf", data = base64Pdf)

                val promptText = """
                    You are an expert exam paper parser. Extract the questions, multiple-choice options, correct answers, and detailed explanations from this PDF.
                    
                    Return your response strictly as a JSON object adhering to this schema:
                    {
                      "title": "A descriptive title of this exam based on the paper (e.g., JEE Advanced 2023 Physics, SAT Geometry Session)",
                      "category": "Broad Subject category (e.g. Physics, Chemistry, UPSC, SAT, Math, Biology, Civics)",
                      "timeLimitMinutes": Suggested time limit in minutes (integer, default to 15 if not apparent, maximum 60),
                      "questions": [
                        {
                          "questionText": "Full text of the question",
                          "options": [
                            "A) option content",
                            "B) option content",
                            "C) option content",
                            "D) option content"
                          ],
                          "correctAnswer": "A", // MUST be exactly 'A', 'B', 'C', or 'D' corresponding to the correct option
                          "explanation": "Detailed step-by-step solution and explanation of why it is correct and others are wrong.",
                          "category": "Sub-topic of this specific question",
                          "difficulty": "Easy", // MUST be 'Easy', 'Medium', or 'Hard'
                        }
                      ]
                    }

                    Rules:
                    1. If the PDF lacks multiple-choice options, generate highly plausible and realistic options (exactly 4, named A), B), C), D)) so we can conduct a multiple-choice mock test.
                    2. If a question is open-ended or numerical, turn it into multiple choice with realistic options, and explain the correct solution.
                    3. Output only valid, raw JSON. Do not wrap the JSON in markdown code blocks like ```json ... ```.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(
                                GeminiPart(text = promptText),
                                GeminiPart(inlineData = inlineData)
                            )
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = "You are a professional mock test generator. Parse previous year papers and answers and output structured JSON only."))
                    )
                )

                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _parsingError.value = "Gemini API Key is missing. Please configure it in the Secrets panel."
                    _isParsing.value = false
                    return@launch
                }

                val response = GeminiApiClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = apiKey,
                    request = request
                )

                val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (rawJson == null) {
                    _parsingError.value = "Gemini did not return any parseable content."
                    _isParsing.value = false
                    return@launch
                }

                parseAndSaveExam(rawJson)

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error parsing PDF", e)
                _parsingError.value = "Error generating test: ${e.localizedMessage ?: "Unknown network error."}"
                _isParsing.value = false
            }
        }
    }

    fun createMockTestFromText(title: String, category: String, textContent: String) {
        if (textContent.isBlank()) return
        _isParsing.value = true
        _parsingError.value = null
        _currentScreen.value = AppScreen.LOADING_EXAM

        viewModelScope.launch {
            try {
                val promptText = """
                    You are an expert exam paper parser. You are given a text containing some questions and answers.
                    Extract these questions, multiple-choice options, correct answers, and detailed explanations.
                    
                    The exam details are:
                    - Title: $title
                    - Category: $category
                    
                    User Content containing Questions and Answers:
                    $textContent

                    Return your response strictly as a JSON object adhering to this schema:
                    {
                      "title": "$title",
                      "category": "$category",
                      "timeLimitMinutes": 15,
                      "questions": [
                        {
                          "questionText": "Full text of the question",
                          "options": [
                            "A) option content",
                            "B) option content",
                            "C) option content",
                            "D) option content"
                          ],
                          "correctAnswer": "A", // MUST be exactly 'A', 'B', 'C', or 'D' corresponding to the correct option
                          "explanation": "Detailed step-by-step solution and explanation of why it is correct and others are wrong.",
                          "category": "Sub-topic of this specific question",
                          "difficulty": "Easy", // MUST be 'Easy', 'Medium', or 'Hard'
                        }
                      ]
                    }

                    Rules:
                    1. If options are missing, generate exactly 4 plausible distractors (A), B), C), D)).
                    2. Output only valid, raw JSON. Do not wrap in markdown code blocks.
                """.trimIndent()

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = promptText))
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(
                        responseMimeType = "application/json",
                        temperature = 0.2f
                    ),
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = "You are a professional mock test generator. Parse the input text and output structured JSON only."))
                    )
                )

                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _parsingError.value = "Gemini API Key is missing. Please configure it in the Secrets panel."
                    _isParsing.value = false
                    return@launch
                }

                val response = GeminiApiClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = apiKey,
                    request = request
                )

                val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (rawJson == null) {
                    _parsingError.value = "Gemini did not return any parseable content."
                    _isParsing.value = false
                    return@launch
                }

                parseAndSaveExam(rawJson)

            } catch (e: Exception) {
                Log.e("MainViewModel", "Error parsing text", e)
                _parsingError.value = "Error generating test: ${e.localizedMessage ?: "Unknown network error."}"
                _isParsing.value = false
            }
        }
    }

    private suspend fun parseAndSaveExam(rawJson: String) {
        val parsed = MoshiUtils.deserializeParsedExam(rawJson)
        if (parsed == null || parsed.questions.isEmpty()) {
            _parsingError.value = "Failed to parse structured questions. Please verify your PDF format."
            _isParsing.value = false
            return
        }

        val questions = parsed.questions.mapIndexed { idx, pq ->
            Question(
                id = "q_${UUID.randomUUID()}_$idx",
                questionText = pq.questionText,
                options = pq.options,
                correctAnswer = pq.correctAnswer.trim().uppercase(),
                explanation = pq.explanation,
                category = pq.category,
                difficulty = pq.difficulty
            )
        }

        val newExam = MockExam(
            id = "exam_${UUID.randomUUID()}",
            title = parsed.title,
            category = parsed.category,
            timeLimitMinutes = parsed.timeLimitMinutes,
            questionsJson = MoshiUtils.serializeQuestions(questions)
        )

        repository.insertExam(newExam)
        _isParsing.value = false

        // Start this newly generated exam immediately!
        startExam(newExam)
    }

    // --- Mock Test Session Execution ---
    fun startExam(exam: MockExam) {
        _activeExam.value = exam
        val questions = MoshiUtils.deserializeQuestions(exam.questionsJson)
        _activeQuestions.value = questions
        _currentQuestionIndex.value = 0
        _userAnswers.value = emptyMap()
        _bookmarkedQuestions.value = emptySet()
        examDurationSeconds = exam.timeLimitMinutes * 60
        _timeRemainingSeconds.value = examDurationSeconds
        _isTimerRunning.value = true
        _currentScreen.value = AppScreen.TEST_SESSION
        
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_timeRemainingSeconds.value > 0 && _isTimerRunning.value) {
                delay(1000L)
                _timeRemainingSeconds.value -= 1
            }
            if (_timeRemainingSeconds.value == 0 && _isTimerRunning.value) {
                submitExam()
            }
        }
    }

    fun pauseResumeTimer() {
        _isTimerRunning.value = !_isTimerRunning.value
        if (_isTimerRunning.value) {
            startTimer()
        } else {
            timerJob?.cancel()
        }
    }

    private fun stopTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun selectAnswer(questionIndex: Int, optionLetter: String) {
        val currentAnswers = _userAnswers.value.toMutableMap()
        currentAnswers[questionIndex] = optionLetter
        _userAnswers.value = currentAnswers
    }

    fun toggleBookmark(questionIndex: Int) {
        val current = _bookmarkedQuestions.value.toMutableSet()
        if (current.contains(questionIndex)) {
            current.remove(questionIndex)
        } else {
            current.add(questionIndex)
        }
        _bookmarkedQuestions.value = current
    }

    fun nextQuestion() {
        if (_currentQuestionIndex.value < _activeQuestions.value.size - 1) {
            _currentQuestionIndex.value += 1
        }
    }

    fun prevQuestion() {
        if (_currentQuestionIndex.value > 0) {
            _currentQuestionIndex.value -= 1
        }
    }

    fun jumpToQuestion(index: Int) {
        if (index in 0 until _activeQuestions.value.size) {
            _currentQuestionIndex.value = index
        }
    }

    fun submitExam() {
        stopTimer()
        val exam = _activeExam.value ?: return
        val questions = _activeQuestions.value
        val answers = _userAnswers.value

        var score = 0
        questions.forEachIndexed { idx, q ->
            val userAnswer = answers[idx]?.trim()?.uppercase()
            val correctAnswer = q.correctAnswer.trim().uppercase()
            if (userAnswer == correctAnswer) {
                score++
            }
        }

        val timeTakenSeconds = examDurationSeconds - _timeRemainingSeconds.value
        val percentage = if (questions.isNotEmpty()) (score.toFloat() / questions.size) * 100f else 0f

        val attempt = TestAttempt(
            examId = exam.id,
            examTitle = exam.title,
            examCategory = exam.category,
            score = score,
            totalQuestions = questions.size,
            timeTakenSeconds = timeTakenSeconds.toLong(),
            answersJson = MoshiUtils.serializeAnswers(answers),
            scorePercentage = percentage
        )

        viewModelScope.launch {
            repository.insertAttempt(attempt)
            // Query latest attempts to show first
            _selectedAttempt.value = attempt
            _selectedAttemptExam.value = exam
            _currentScreen.value = AppScreen.ANALYTICS
        }
    }

    // --- View History / Saved Attempt Details ---
    fun selectAttemptDetails(attempt: TestAttempt) {
        _selectedAttempt.value = attempt
        _selectedAttemptExam.value = null

        viewModelScope.launch {
            val exam = repository.getExamById(attempt.examId)
            _selectedAttemptExam.value = exam
            _currentScreen.value = AppScreen.ATTEMPT_DETAILS
        }
    }

    fun deleteAttempt(id: Int) {
        viewModelScope.launch {
            repository.deleteAttempt(id)
            if (_selectedAttempt.value?.id == id) {
                _currentScreen.value = AppScreen.HOME
            }
        }
    }

    fun deleteExam(id: String) {
        viewModelScope.launch {
            repository.deleteExam(id)
        }
    }

    // --- AI Coach Doubt Clearing ---
    fun askAiCoach(question: Question, userAnswer: String, followUpQuery: String? = null) {
        _currentCoachQuestionId.value = question.id
        _aiCoachLoading.value = true
        _aiCoachResponse.value = null

        viewModelScope.launch {
            try {
                val promptText = if (followUpQuery.isNullOrBlank()) {
                    """
                        You are an encouraging and expert Academic Coach. Explain this question in high detail.
                        
                        Question: ${question.questionText}
                        Options:
                        ${question.options.joinToString("\n")}
                        
                        Correct Answer: ${question.correctAnswer}
                        User's Answer: ${if (userAnswer.isEmpty()) "Skipped" else userAnswer}
                        
                        Academic Explanation:
                        ${question.explanation}
                        
                        Please write a friendly, highly intuitive breakdown of the core concept. First congratulate them if they got it right, or gently correct their misconception if they got it wrong. Use analogies and break the problem down into simple, memorable steps so they never forget this concept!
                    """.trimIndent()
                } else {
                    """
                        You are an encouraging and expert Academic Coach. The user has a specific doubt about this question.
                        
                        Question: ${question.questionText}
                        Options:
                        ${question.options.joinToString("\n")}
                        
                        Correct Answer: ${question.correctAnswer}
                        User's Answer: ${if (userAnswer.isEmpty()) "Skipped" else userAnswer}
                        
                        Previous Explanation:
                        ${question.explanation}
                        
                        User's follow-up doubt:
                        $followUpQuery
                        
                        Address their doubt directly with crystal clear reasoning, math if required, or simple conceptual breakdowns. Keep the tone friendly, academic, and supportive.
                    """.trimIndent()
                }

                val request = GeminiRequest(
                    contents = listOf(
                        GeminiContent(
                            parts = listOf(GeminiPart(text = promptText))
                        )
                    ),
                    generationConfig = GeminiGenerationConfig(temperature = 0.5f),
                    systemInstruction = GeminiContent(
                        parts = listOf(GeminiPart(text = "You are a friendly and world-class academic tutor helping a student master concepts from past exam papers."))
                    )
                )

                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                    _aiCoachResponse.value = "Gemini API Key is missing. Please configure it in the Secrets panel."
                    _aiCoachLoading.value = false
                    return@launch
                }

                val response = GeminiApiClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = apiKey,
                    request = request
                )

                val coachOutput = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                _aiCoachResponse.value = coachOutput ?: "No response from AI Coach."
                _aiCoachLoading.value = false

            } catch (e: Exception) {
                _aiCoachResponse.value = "AI Coach could not connect: ${e.localizedMessage ?: "Network error."}"
                _aiCoachLoading.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
