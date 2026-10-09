package com.example.intellipatassignment.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.intellipatassignment.R
import com.example.intellipatassignment.domain.model.Course
import com.example.intellipatassignment.domain.model.Lesson
import com.example.intellipatassignment.ui.common.ConfirmDialog
import com.example.intellipatassignment.ui.common.FullScreenLoading
import com.example.intellipatassignment.ui.common.MessageState
import com.example.intellipatassignment.ui.common.ThinProgress
import com.example.intellipatassignment.ui.theme.AppColors
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CourseDetailsRoute(
    courseId: Int,
    onBack: () -> Unit,
    viewModel: CourseDetailsViewModel = koinViewModel(parameters = { parametersOf(courseId) }),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHost = remember { SnackbarHostState() }
    val failureMessage = stringResource(R.string.error_mark_failed)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CourseDetailsEvent.MarkCompletedFailed -> snackbarHost.showSnackbar(failureMessage)
            }
        }
    }

    CourseDetailsScreen(state, snackbarHost, onBack, viewModel::onMarkCompleted)
}

@Composable
fun CourseDetailsScreen(
    state: CourseDetailsUiState,
    snackbarHost: SnackbarHostState,
    onBack: () -> Unit,
    onMarkCompleted: (Int) -> Unit,
) {
    var lessonToComplete by remember { mutableStateOf<Lesson?>(null) }

    lessonToComplete?.let { lesson ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_complete_title),
            message = stringResource(R.string.confirm_complete_message, lesson.title),
            confirmLabel = stringResource(R.string.confirm),
            onConfirm = {
                onMarkCompleted(lesson.id)
                lessonToComplete = null
            },
            onDismiss = { lessonToComplete = null },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            IconButton(onClick = onBack, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
            }
            when (state) {
                CourseDetailsUiState.Loading -> FullScreenLoading()
                CourseDetailsUiState.NotFound -> MessageState(
                    icon = Icons.Filled.Search,
                    title = stringResource(R.string.course_not_found),
                    message = stringResource(R.string.course_not_found_message),
                )
                is CourseDetailsUiState.Success -> CourseContent(state.course, onMarkCompleted = { lessonToComplete = it })
            }
        }
    }
}

@Composable
private fun CourseContent(course: Course, onMarkCompleted: (Lesson) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "header") { CourseHeader(course) }
        item(key = "lessons-title") {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(stringResource(R.string.lessons), style = MaterialTheme.typography.titleLarge)
                Text(
                    stringResource(R.string.lessons_count, course.lessonCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        itemsIndexed(course.lessons, key = { _, l -> l.id }) { index, lesson ->
            LessonRow(index + 1, lesson, onMarkCompleted = { onMarkCompleted(lesson) })
            if (index < course.lessons.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 64.dp, end = 20.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
}

@Composable
private fun CourseHeader(course: Course) {
    Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(course.title, style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.course_instructor, course.instructor),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    stringResource(R.string.progress_summary, course.completedLessonCount, course.lessonCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(R.string.course_progress, course.progress), style = MaterialTheme.typography.labelLarge)
            }
            ThinProgress(course.progress, AppColors.progress, height = 8.dp)
        }
    }
}

@Composable
private fun LessonRow(number: Int, lesson: Lesson, onMarkCompleted: () -> Unit) {
    val statusColor = when {
        lesson.isSyncPending -> AppColors.pending
        lesson.isCompleted -> AppColors.progress
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        StatusDot(number, lesson)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(lesson.title, style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(
                    when {
                        lesson.isSyncPending -> R.string.lesson_sync_pending
                        lesson.isCompleted -> R.string.lesson_completed
                        else -> R.string.lesson_pending
                    },
                ),
                style = MaterialTheme.typography.labelMedium,
                color = statusColor,
            )
        }
        if (!lesson.isCompleted) {
            TextButton(onClick = onMarkCompleted) {
                Text(stringResource(R.string.mark_complete), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun StatusDot(number: Int, lesson: Lesson) {
    val done = AppColors.progress
    val pending = AppColors.pending
    val description = stringResource(
        when {
            lesson.isSyncPending -> R.string.lesson_a11y_syncing
            lesson.isCompleted -> R.string.lesson_a11y_completed
            else -> R.string.lesson_a11y_pending
        },
        number,
    )
    val base = Modifier.size(28.dp).semantics(mergeDescendants = true) { contentDescription = description }
    when {
        lesson.isSyncPending -> Box(base.border(1.5.dp, pending, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Refresh, null, tint = pending, modifier = Modifier.size(15.dp))
        }
        lesson.isCompleted -> Box(base.background(done, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        else -> Box(base.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape), contentAlignment = Alignment.Center) {
            Text(number.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
