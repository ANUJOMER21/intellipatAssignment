package com.example.intellipatassignment.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.intellipatassignment.R
import com.example.intellipatassignment.core.ErrorKind
import com.example.intellipatassignment.ui.theme.Ink

@Composable
fun ErrorKind.message(): String = stringResource(
    when (this) {
        ErrorKind.Offline -> R.string.error_offline
        ErrorKind.Network -> R.string.error_network
        ErrorKind.Unauthorized -> R.string.error_unauthorized
        ErrorKind.Forbidden -> R.string.error_forbidden
        ErrorKind.NotFound -> R.string.error_not_found
        ErrorKind.Server -> R.string.error_server
        ErrorKind.BadResponse -> R.string.error_bad_response
        ErrorKind.Unknown -> R.string.error_unknown
    },
)

@Composable
fun BrandMark(modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Canvas(modifier.size(size)) {
        drawRoundRect(Ink, size = this.size, cornerRadius = CornerRadius(size.toPx() * 0.28f))
        val barW = this.size.width * 0.14f
        val gap = this.size.width * 0.09f
        val baseY = this.size.height * 0.74f
        val startX = (this.size.width - (barW * 3 + gap * 2)) / 2
        listOf(0.2f to Color.White, 0.34f to Color.White, 0.5f to Color(0xFF5EEAD4)).forEachIndexed { i, (h, c) ->
            val height = this.size.height * h
            drawRoundRect(
                c,
                topLeft = Offset(startX + i * (barW + gap), baseY - height),
                size = Size(barW, height),
                cornerRadius = CornerRadius(barW * 0.3f),
            )
        }
    }
}

@Composable
fun ThinProgress(
    percent: Int,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
) {
    val animated by animateFloatAsState(percent / 100f, tween(600), label = "progress")
    LinearProgressIndicator(
        progress = { animated },
        modifier = modifier.fillMaxWidth().height(height),
        color = color,
        trackColor = MaterialTheme.colorScheme.outlineVariant,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

@Composable
fun FullScreenLoading(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
fun CourseListSkeleton(modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
        label = "pulse",
    )
    val block = MaterialTheme.colorScheme.surfaceVariant
    Column(modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(4) {
            Surface(
                modifier = Modifier.fillMaxWidth().alpha(pulse),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.fillMaxWidth(0.55f).height(16.dp).background(block, RoundedCornerShape(4.dp)))
                        Box(Modifier.fillMaxWidth(0.35f).height(12.dp).background(block, RoundedCornerShape(4.dp)))
                        Box(Modifier.fillMaxWidth().height(6.dp).background(block, CircleShape))
                    }
                }
            }
        }
    }
}

@Composable
fun MessageState(
    icon: ImageVector,
    title: String,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String = stringResource(R.string.retry),
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp)) }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        if (message.isNotBlank()) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (onAction != null) {
            Button(onClick = onAction, shape = MaterialTheme.shapes.medium, modifier = Modifier.padding(top = 10.dp)) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        shape = MaterialTheme.shapes.large,
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    confirmLabel,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
