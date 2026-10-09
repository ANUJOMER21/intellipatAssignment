package com.example.intellipatassignment.ui.courses

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.intellipatassignment.R
import com.example.intellipatassignment.domain.model.Course
import com.example.intellipatassignment.ui.common.ConfirmDialog
import com.example.intellipatassignment.ui.common.CourseListSkeleton
import com.example.intellipatassignment.ui.common.MessageState
import com.example.intellipatassignment.ui.common.ThinProgress
import com.example.intellipatassignment.ui.common.message
import com.example.intellipatassignment.ui.theme.AppColors
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CoursesRoute(
    onCourseClick: (Int) -> Unit,
    onLoggedOut: () -> Unit,
    viewModel: CoursesViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()
    val rejected by viewModel.rejectedLessons.collectAsStateWithLifecycle()

    CoursesScreen(
        state = state,
        isOnline = isOnline,
        rejectedLessons = rejected,
        onRefresh = { viewModel.refresh() },
        onDismissRejected = viewModel::dismissRejectedLessons,
        onCourseClick = onCourseClick,
        onLogout = { viewModel.logout(onLoggedOut) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoursesScreen(
    state: CoursesUiState,
    isOnline: Boolean,
    rejectedLessons: List<String>,
    onRefresh: () -> Unit,
    onDismissRejected: () -> Unit,
    onCourseClick: (Int) -> Unit,
    onLogout: () -> Unit,
) {
    val snackbarHost = remember { SnackbarHostState() }
    if (rejectedLessons.isNotEmpty()) {
        val message = stringResource(R.string.sync_rejected_message, rejectedLessons.joinToString())
        val actionLabel = stringResource(R.string.dismiss)
        LaunchedEffect(rejectedLessons) {
            snackbarHost.showSnackbar(message, actionLabel, duration = SnackbarDuration.Indefinite)
            onDismissRejected()
        }
    }

    var confirmLogout by remember { mutableStateOf(false) }
    if (confirmLogout) {
        ConfirmDialog(
            title = stringResource(R.string.logout_title),
            message = stringResource(R.string.logout_message),
            confirmLabel = stringResource(R.string.logout),
            destructive = true,
            onConfirm = {
                confirmLogout = false
                onLogout()
            },
            onDismiss = { confirmLogout = false },
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Header(state, onLogout = { confirmLogout = true })

            when (state) {
                CoursesUiState.Loading -> CourseListSkeleton()
                CoursesUiState.Empty -> MessageState(
                    icon = Icons.Filled.Info,
                    title = stringResource(R.string.courses_empty_title),
                    message = stringResource(R.string.courses_empty_message),
                    actionLabel = stringResource(R.string.refresh),
                    onAction = onRefresh,
                )
                is CoursesUiState.Error -> MessageState(
                    icon = Icons.Filled.Warning,
                    title = stringResource(R.string.courses_error_title),
                    message = state.kind.message(),
                    onAction = onRefresh,
                )
                is CoursesUiState.Success -> {
                    when {
                        !isOnline -> Notice(stringResource(R.string.offline_banner))
                        state.refreshError != null -> Notice(stringResource(R.string.refresh_failed_banner))
                        state.isStale -> Notice(stringResource(R.string.stale_banner))
                    }
                    PullToRefreshBox(isRefreshing = state.isRefreshing, onRefresh = onRefresh) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            items(state.courses, key = { it.id }) { course ->
                                CourseCard(course, onContinue = { onCourseClick(course.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(state: CoursesUiState, onLogout: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.my_courses), style = MaterialTheme.typography.headlineMedium)
            if (state is CoursesUiState.Success) {
                Text(
                    stringResource(
                        R.string.courses_summary,
                        state.courses.size,
                        state.courses.sumOf { it.completedLessonCount },
                        state.courses.sumOf { it.lessonCount },
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                state.lastSyncedAt?.let {
                    Text(
                        stringResource(R.string.updated_at, relativeTime(it)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        IconButton(onClick = onLogout) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, stringResource(R.string.logout), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun relativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    if (now - timestamp < DateUtils.MINUTE_IN_MILLIS) return stringResource(R.string.updated_just_now)
    return DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.MINUTE_IN_MILLIS).toString()
}

@Composable
private fun Notice(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp).fillMaxWidth(),
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun CourseCard(course: Course, onContinue: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(course.title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.course_instructor, course.instructor),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(
                            stringResource(R.string.course_lessons, course.lessonCount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(stringResource(R.string.course_progress, course.progress), style = MaterialTheme.typography.labelLarge)
                    }
                    ThinProgress(course.progress, AppColors.progress)
                }
                Button(
                    onClick = onContinue,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                ) { Text(stringResource(R.string.continue_course), style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}
