package com.example.ui

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MockExam
import com.example.data.MoshiUtils
import com.example.data.Question
import com.example.data.TestAttempt
import com.example.ui.theme.*
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainScreen(
    viewModel: MainViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val isParsing by viewModel.isParsing.collectAsStateWithLifecycle()
    val parsingError by viewModel.parsingError.collectAsStateWithLifecycle()

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.HOME -> DashboardScreen(viewModel = viewModel)
                AppScreen.LOADING_EXAM -> LoadingExamScreen(
                    isParsing = isParsing,
                    error = parsingError,
                    onBackToHome = { viewModel.navigateTo(AppScreen.HOME) }
                )
                AppScreen.TEST_SESSION -> TestSessionScreen(viewModel = viewModel)
                AppScreen.ANALYTICS -> AnalyticsScreen(viewModel = viewModel, isDetailsMode = false)
                AppScreen.ATTEMPT_DETAILS -> AnalyticsScreen(viewModel = viewModel, isDetailsMode = true)
            }
        }
    }
}

// ==========================================
// 1. DASHBOARD / HOME SCREEN
// ==========================================
@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val allExams by viewModel.allExams.collectAsStateWithLifecycle()
    val allAttempts by viewModel.allAttempts.collectAsStateWithLifecycle()

    // Upload & Text input form states
    var textTitle by remember { mutableStateOf("") }
    var textCategory by remember { mutableStateOf("") }
    var textContent by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: PDF File, 1: Paste Q&A

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.createMockTestFromPdf(context, uri)
        } else {
            Toast.makeText(context, "No file selected", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- 1. HERO BANNER ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.School,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "ExamFlow AI Mock Coach",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ready for a challenge? Extract PYQs instantly from PDFs or paste text and practice offline.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // --- 2. LEARNING STATS ---
        item {
            LearningStatsCard(allAttempts = allAttempts)
        }

        // --- 3. GENERATION PANEL ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Build Mock Test Paper",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Tab Selection
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeTab == 0) MintGreen else Color.Transparent)
                                .clickable { activeTab = 0 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Upload PYQ PDF",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (activeTab == 1) MintGreen else Color.Transparent)
                                .clickable { activeTab = 1 },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Paste Q&A Text",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (activeTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (activeTab == 0) {
                        // PDF Uploader Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .border(
                                    1.dp,
                                    MintGreen.copy(alpha = 0.4f),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { pdfPickerLauncher.launch("application/pdf") },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.UploadFile,
                                    contentDescription = "Upload",
                                    tint = MintGreen,
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(
                                    text = "Select Past Year Question Paper PDF",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Supports question-and-answer papers or solutions",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else {
                        // Paste Text Form
                        OutlinedTextField(
                            value = textTitle,
                            onValueChange = { textTitle = it },
                            label = { Text("Exam Title (e.g. UPSC CSE Polity 2023)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintGreen)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = textCategory,
                            onValueChange = { textCategory = it },
                            label = { Text("Category/Subject (e.g. Physics, Civics, SAT)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintGreen)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = textContent,
                            onValueChange = { textContent = it },
                            label = { Text("Paste Questions & Answers here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            maxLines = 10,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MintGreen)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (textTitle.isBlank() || textCategory.isBlank() || textContent.isBlank()) {
                                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.createMockTestFromText(textTitle, textCategory, textContent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
                        ) {
                            Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Generate AI Mock Exam", color = Color.White)
                        }
                    }
                }
            }
        }

        // --- 4. PRELOADED EXAMS ---
        item {
            Text(
                text = "Mock Papers & Question Banks",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (allExams.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MintGreen)
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(allExams) { exam ->
                        ExamPaperCard(exam = exam, onStart = { viewModel.startExam(exam) }, onDelete = { viewModel.deleteExam(exam.id) })
                    }
                }
            }
        }

        // --- 5. RECENT TEST HISTORY ---
        item {
            Text(
                text = "Your Performance History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (allAttempts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "No tests attempted yet.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Generate a test and submit your answers to see metrics.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        } else {
            items(allAttempts) { attempt ->
                AttemptHistoryRow(
                    attempt = attempt,
                    onViewDetails = { viewModel.selectAttemptDetails(attempt) },
                    onDelete = { viewModel.deleteAttempt(attempt.id) }
                )
            }
        }

        // --- 6. FOOTER ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Made with",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "💕",
                        fontSize = 14.sp
                    )
                    Text(
                        text = "by Kaif",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun LearningStatsCard(allAttempts: List<TestAttempt>) {
    val totalAttempts = allAttempts.size
    val averageScore = if (totalAttempts > 0) {
        allAttempts.map { it.scorePercentage }.average().toFloat()
    } else 0f

    val totalQuestionsSolved = allAttempts.sumOf { it.totalQuestions }
    val totalCorrect = allAttempts.sumOf { it.score }

    val accuracyStr = if (totalQuestionsSolved > 0) {
        String.format("%.1f%%", (totalCorrect.toFloat() / totalQuestionsSolved) * 100f)
    } else "0%"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overall Learning Statistics",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatSubCol(label = "Tests Taken", value = totalAttempts.toString(), icon = Icons.Rounded.ContentPaste)
                StatSubCol(label = "Avg Score", value = String.format("%.0f%%", averageScore), icon = Icons.Rounded.EmojiEvents)
                StatSubCol(label = "Accuracy", value = accuracyStr, icon = Icons.Rounded.CheckCircle)
                StatSubCol(label = "Solves", value = totalQuestionsSolved.toString(), icon = Icons.Rounded.BarChart)
            }
        }
    }
}

@Composable
fun StatSubCol(label: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MintGreen, modifier = Modifier.size(20.dp))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ExamPaperCard(exam: MockExam, onStart: () -> Unit, onDelete: () -> Unit) {
    val questions = remember(exam.questionsJson) { MoshiUtils.deserializeQuestions(exam.questionsJson) }
    val totalQuestions = questions.size

    Card(
        modifier = Modifier
            .width(220.dp)
            .shadow(1.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MintGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = exam.category,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MintGreen
                        )
                    }

                    // Delete button for custom exams (sample exams have custom IDs starting with "sample_")
                    if (!exam.id.startsWith("sample_")) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Exam",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = exam.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.height(38.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Quiz,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "$totalQuestions Qs",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${exam.timeLimitMinutes}m",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
                ) {
                    Text("Take Mock Test", fontSize = 12.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AttemptHistoryRow(attempt: TestAttempt, onViewDetails: () -> Unit, onDelete: () -> Unit) {
    val dateStr = remember(attempt.timestamp) {
        val sdf = SimpleDateFormat("MMM dd, yyyy · hh:mm a", Locale.getDefault())
        sdf.format(Date(attempt.timestamp))
    }

    val scorePercentage = attempt.scorePercentage
    val badgeColor = when {
        scorePercentage >= 80f -> SuccessGreen
        scorePercentage >= 50f -> WarningOrange
        else -> ErrorRed
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp))
            .clickable { onViewDetails() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(badgeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${scorePercentage.toInt()}%",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = attempt.examTitle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = attempt.examCategory,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MintGreen
                    )
                    Text(
                        text = "•",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$dateStr",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "${attempt.score}/${attempt.totalQuestions}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Attempt",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// 2. LOADING & PARSING SCREEN
// ==========================================
@Composable
fun LoadingExamScreen(
    isParsing: Boolean,
    error: String?,
    onBackToHome: () -> Unit
) {
    var tipIndex by remember { mutableIntStateOf(0) }
    val tips = listOf(
        "Did you know? Reviewing detailed step-by-step solutions with AI reduces future misconceptions by 60%.",
        "Tip: Use the 'Review Later' bookmarking tool during tough exams to skip and return.",
        "Tip: Active recall is the #1 scientifically proven method for test preparation.",
        "Fun Fact: Gemini reads structural text inside PDF diagrams to extract complete question formulas.",
        "Tutor Advice: Read all 4 distractors carefully. Often, subtle conceptual differences are tested."
    )

    LaunchedEffect(isParsing) {
        while (isParsing) {
            delay(3500L)
            tipIndex = (tipIndex + 1) % tips.size
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isParsing) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(100.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        color = MintGreen,
                        strokeWidth = 6.dp
                    )
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MintGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text(
                    text = "Gemini is Constructing Your Mock Exam",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Analyzing PYQ diagrams, identifying correct keys, and generating logical academic explanations...",
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Tips Slider Container
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Outlined.Lightbulb, contentDescription = null, tint = WarningOrange)
                            Text(text = "Study Coach Tip", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WarningOrange)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        AnimatedContent(
                            targetState = tips[tipIndex],
                            transitionSpec = {
                                fadeIn(animationSpec = spring()) togetherWith fadeOut(animationSpec = spring())
                            }, label = "TipSlider"
                        ) { text ->
                            Text(
                                text = text,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            } else if (error != null) {
                Icon(
                    imageVector = Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    text = "Mock Paper Generation Failed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = error,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onBackToHome,
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
                ) {
                    Text("Return to Dashboard", color = Color.White)
                }
            }
        }
    }
}

// ==========================================
// 3. ACTIVE TEST SESSION SCREEN
// ==========================================
@Composable
fun TestSessionScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val exam by viewModel.activeExam.collectAsStateWithLifecycle()
    val questions by viewModel.activeQuestions.collectAsStateWithLifecycle()
    val currentIndex by viewModel.currentQuestionIndex.collectAsStateWithLifecycle()
    val userAnswers by viewModel.userAnswers.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarkedQuestions.collectAsStateWithLifecycle()
    val timeRemaining by viewModel.timeRemainingSeconds.collectAsStateWithLifecycle()
    val isTimerRunning by viewModel.isTimerRunning.collectAsStateWithLifecycle()

    if (exam == null || questions.isEmpty()) return

    val currentQuestion = questions[currentIndex]
    val minutes = timeRemaining / 60
    val seconds = timeRemaining % 60
    val formattedTime = String.format("%02d:%02d", minutes, seconds)
    val timerAlertColor = if (timeRemaining < 60) ErrorRed else if (timeRemaining < 180) WarningOrange else MintGreen

    var showSubmitConfirmation by remember { mutableStateOf(false) }

    if (showSubmitConfirmation) {
        AlertDialog(
            onDismissRequest = { showSubmitConfirmation = false },
            title = { Text("Submit Mock Test?") },
            text = {
                val answeredCount = userAnswers.size
                val skippedCount = questions.size - answeredCount
                Text("You have answered $answeredCount of ${questions.size} questions. Are you sure you want to finish and view your analytics?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSubmitConfirmation = false
                        viewModel.submitExam()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MintGreen)
                ) {
                    Text("Submit & Score", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSubmitConfirmation = false }) {
                    Text("Go Back")
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- TOP EXAM BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(2.dp)
                .background(SlateNavyDark)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = {
                    viewModel.navigateTo(AppScreen.HOME)
                }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Cancel", tint = Color.White)
                }

                Text(
                    text = exam!!.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                )

                // Timer Display
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(timerAlertColor.copy(alpha = 0.15f))
                        .border(1.dp, timerAlertColor, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .clickable { viewModel.pauseResumeTimer() }
                ) {
                    Icon(
                        imageVector = if (isTimerRunning) Icons.Default.Timer else Icons.Default.PlayArrow,
                        contentDescription = "Timer",
                        tint = timerAlertColor,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formattedTime,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = timerAlertColor
                    )
                }
            }
        }

        // --- SUB HEADER WITH PROGRESS ---
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Question ${currentIndex + 1} of ${questions.size}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Bookmark toggle
                    IconButton(onClick = { viewModel.toggleBookmark(currentIndex) }) {
                        Icon(
                            imageVector = if (bookmarks.contains(currentIndex)) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (bookmarks.contains(currentIndex)) WarningOrange else MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress bar
            LinearProgressIndicator(
                progress = { (currentIndex + 1).toFloat() / questions.size },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape),
                color = MintGreen,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // --- MAIN QUESTION AREA ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Question text card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(1.dp, RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SoftBlue.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentQuestion.category,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftBlue
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when (currentQuestion.difficulty) {
                                            "Hard" -> ErrorRed.copy(alpha = 0.15f)
                                            "Easy" -> SuccessGreen.copy(alpha = 0.15f)
                                            else -> WarningOrange.copy(alpha = 0.15f)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = currentQuestion.difficulty,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (currentQuestion.difficulty) {
                                        "Hard" -> ErrorRed
                                        "Easy" -> SuccessGreen
                                        else -> WarningOrange
                                    }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = currentQuestion.questionText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Options list
                currentQuestion.options.forEach { option ->
                    val optionLetter = option.firstOrNull()?.toString()?.uppercase() ?: ""
                    val isSelected = userAnswers[currentIndex] == optionLetter

                    val cardBorder = if (isSelected) BorderStroke(2.dp, MintGreen) else null
                    val cardBg = if (isSelected) MintGreen.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RoundedCornerShape(10.dp))
                            .clickable { viewModel.selectAnswer(currentIndex, optionLetter) },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        border = cardBorder
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MintGreen else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = optionLetter,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = option,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.CheckCircle,
                                    contentDescription = null,
                                    tint = MintGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- QUICK QUESTION MAP ACCORDION/GRID ---
                Text(
                    text = "Exam Navigator Grid",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    questions.forEachIndexed { idx, q ->
                        val isCurrent = currentIndex == idx
                        val isAnswered = userAnswers.containsKey(idx)
                        val isBookmarked = bookmarks.contains(idx)

                        val cellBg = when {
                            isCurrent -> SoftBlue
                            isAnswered -> MintGreen
                            isBookmarked -> WarningOrange
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        }

                        val textColor = if (isCurrent || isAnswered || isBookmarked) Color.White else MaterialTheme.colorScheme.onSurface

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(cellBg)
                                .border(
                                    width = if (isCurrent) 2.dp else 0.dp,
                                    color = if (isCurrent) Color.White else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { viewModel.jumpToQuestion(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (idx + 1).toString(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }

        // --- FOOTER CONTROLS ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp)
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Prev button
                TextButton(
                    onClick = { viewModel.prevQuestion() },
                    enabled = currentIndex > 0
                ) {
                    Icon(imageVector = Icons.Default.ChevronLeft, contentDescription = null)
                    Text("Previous")
                }

                // Submit Test Center Button
                Button(
                    onClick = { showSubmitConfirmation = true },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningOrange)
                ) {
                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Submit Paper", color = Color.White)
                }

                // Next Button
                if (currentIndex < questions.size - 1) {
                    TextButton(
                        onClick = { viewModel.nextQuestion() }
                    ) {
                        Text("Next")
                        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
                    }
                } else {
                    Box(modifier = Modifier.width(80.dp)) // spacer
                }
            }
        }
    }
}

// ==========================================
// 4. PERFORMANCE ANALYTICS & REVIEW SCREEN
// ==========================================
@Composable
fun AnalyticsScreen(viewModel: MainViewModel, isDetailsMode: Boolean) {
    val attempt by viewModel.selectedAttempt.collectAsStateWithLifecycle()
    val exam by viewModel.selectedAttemptExam.collectAsStateWithLifecycle()

    if (attempt == null) return

    val questions = remember(exam) {
        exam?.let { MoshiUtils.deserializeQuestions(it.questionsJson) } ?: emptyList()
    }
    val userAnswers = remember(attempt) {
        MoshiUtils.deserializeAnswers(attempt!!.answersJson)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. HEADER ROW ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = {
                    viewModel.navigateTo(AppScreen.HOME)
                }) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = if (isDetailsMode) "Attempt Details" else "Performance Scorecard",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = attempt!!.examTitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // --- 2. RADIAL PERCENTAGE CHART ---
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(2.dp, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val scorePercent = attempt!!.scorePercentage
                    val ratingText = when {
                        scorePercent >= 90f -> "Genius Masterclass!"
                        scorePercent >= 75f -> "Outstanding Job!"
                        scorePercent >= 50f -> "Strong Effort!"
                        else -> "Requires Academic Revision"
                    }
                    val ratingColor = when {
                        scorePercent >= 80f -> SuccessGreen
                        scorePercent >= 50f -> WarningOrange
                        else -> ErrorRed
                    }

                    // Circle score chart drawing
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(130.dp)
                    ) {
                        Canvas(modifier = Modifier.size(120.dp)) {
                            // Track background
                            drawArc(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Animated rating arc
                            drawArc(
                                color = ratingColor,
                                startAngle = -90f,
                                sweepAngle = (scorePercent / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${scorePercent.toInt()}%",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Correct",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = ratingText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ratingColor,
                        textAlign = TextAlign.Center
                    )

                    // Stats Grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        SmallMetricCard(
                            label = "Score",
                            value = "${attempt!!.score}/${attempt!!.totalQuestions}",
                            icon = Icons.Outlined.Check
                        )
                        SmallMetricCard(
                            label = "Duration",
                            value = formatTimeSeconds(attempt!!.timeTakenSeconds),
                            icon = Icons.Outlined.Timer
                        )
                        SmallMetricCard(
                            label = "Accuracy",
                            value = "${attempt!!.scorePercentage.toInt()}%",
                            icon = Icons.Outlined.CompassCalibration
                        )
                    }
                }
            }
        }

        // --- 3. TOPIC LEVEL INSIGHTS ---
        if (questions.isNotEmpty()) {
            item {
                Text(
                    text = "Topic Mastery Analysis",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                TopicBreakdownCard(questions = questions, userAnswers = userAnswers)
            }
        }

        // --- 4. QUESTION BY QUESTION ANSWER KEY ---
        item {
            Text(
                text = "Question-by-Question Academic Review",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (questions.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Original exam questions deleted or unavailable.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(questions.size) { idx ->
                val q = questions[idx]
                val userAnswer = userAnswers[idx] ?: ""
                val isCorrect = userAnswer.trim().uppercase() == q.correctAnswer.trim().uppercase()

                QuestionReviewExpandableCard(
                    index = idx,
                    question = q,
                    userAnswer = userAnswer,
                    isCorrect = isCorrect,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun SmallMetricCard(label: String, value: String, icon: ImageVector) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SoftBlue, modifier = Modifier.size(18.dp))
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TopicBreakdownCard(questions: List<Question>, userAnswers: Map<Int, String>) {
    // Group questions by topic/category
    val topicGroups = remember(questions, userAnswers) {
        val groups = mutableMapOf<String, Pair<Int, Int>>() // topic -> Pair(CorrectCount, TotalCount)
        questions.forEachIndexed { idx, q ->
            val uAns = userAnswers[idx] ?: ""
            val corr = if (uAns.trim().uppercase() == q.correctAnswer.trim().uppercase()) 1 else 0
            val prev = groups[q.category] ?: Pair(0, 0)
            groups[q.category] = Pair(prev.first + corr, prev.second + 1)
        }
        groups
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            topicGroups.forEach { (topic, stats) ->
                val (correct, total) = stats
                val percentage = (correct.toFloat() / total) * 100f
                val barColor = if (percentage >= 80f) SuccessGreen else if (percentage >= 50f) WarningOrange else ErrorRed

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = topic,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$correct/$total (${percentage.toInt()}%)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = barColor
                        )
                    }

                    LinearProgressIndicator(
                        progress = { correct.toFloat() / total },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = barColor,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun QuestionReviewExpandableCard(
    index: Int,
    question: Question,
    userAnswer: String,
    isCorrect: Boolean,
    viewModel: MainViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val cardBorder = if (isCorrect) BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f)) else BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f))
    val badgeColor = if (isCorrect) SuccessGreen else ErrorRed
    val badgeText = if (userAnswer.isEmpty()) "Skipped" else if (isCorrect) "Correct" else "Incorrect"

    val aiResponse by viewModel.aiCoachResponse.collectAsStateWithLifecycle()
    val aiLoading by viewModel.aiCoachLoading.collectAsStateWithLifecycle()
    val currentCoachQuestionId by viewModel.currentCoachQuestionId.collectAsStateWithLifecycle()
    var followUpDoubtText by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Clickable to expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(badgeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (index + 1).toString(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }

                Text(
                    text = question.questionText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = if (expanded) Int.MAX_VALUE else 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = badgeColor
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Expanded Area containing Solution Details & AI Coach Chat
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Show options
                    Text(
                        text = "Options:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    question.options.forEach { option ->
                        val optLetter = option.firstOrNull()?.toString()?.uppercase() ?: ""
                        val isUserSelection = optLetter == userAnswer.trim().uppercase()
                        val isCorrectSelection = optLetter == question.correctAnswer.trim().uppercase()

                        val textColor = when {
                            isCorrectSelection -> SuccessGreen
                            isUserSelection -> ErrorRed
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        val optWeight = if (isCorrectSelection || isUserSelection) FontWeight.Bold else FontWeight.Normal

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            if (isCorrectSelection) {
                                Icon(imageVector = Icons.Rounded.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                            } else if (isUserSelection) {
                                Icon(imageVector = Icons.Rounded.Cancel, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(16.dp))
                            } else {
                                Box(modifier = Modifier.size(16.dp)) // space holder
                            }
                            Text(
                                text = option,
                                fontSize = 13.sp,
                                fontWeight = optWeight,
                                color = textColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Base solution text
                    Text(
                        text = "Step-by-Step Explanation:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = question.explanation,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- AI COACH CONVERSATION BOX ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MintGreen.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MintGreen, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = "Ask AI Coach About This",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MintGreen
                                    )
                                }

                                if (aiResponse == null && currentCoachQuestionId != question.id) {
                                    Button(
                                        onClick = { viewModel.askAiCoach(question, userAnswer) },
                                        colors = ButtonDefaults.buttonColors(containerColor = MintGreen),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                        ) {
                                        Text("Ask Coach", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                            }

                            // Loading state inside this specific card
                            if (aiLoading && currentCoachQuestionId == question.id) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MintGreen)
                                    Text(text = "Tutor is writing a conceptual guide...", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            // AI Coach Explains Concept
                            if (aiResponse != null && currentCoachQuestionId == question.id) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Divider(color = MaterialTheme.colorScheme.surfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = aiResponse!!,
                                    fontSize = 12.sp,
                                    lineHeight = 17.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                // Follow up text field
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = followUpDoubtText,
                                        onValueChange = { followUpDoubtText = it },
                                        placeholder = { Text("Still confused? Ask anything...", fontSize = 11.sp) },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MintGreen,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        )
                                    )

                                    IconButton(
                                        onClick = {
                                            if (followUpDoubtText.isNotBlank()) {
                                                viewModel.askAiCoach(question, userAnswer, followUpDoubtText)
                                                followUpDoubtText = ""
                                            }
                                        },
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MintGreen)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Send,
                                            contentDescription = "Send doubt",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// UTILITY FUNCTIONS
// ==========================================
fun formatTimeSeconds(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
