package com.tubetone.trim

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.tubetone.ringtone.RingtoneSlot
import com.tubetone.waveform.WaveformCanvas
import kotlinx.coroutines.launch

/** Result the save handler reports back so TrimScreen can show a Snackbar. */
sealed class SaveResult {
    data class Success(
        val slot: RingtoneSlot,
        val appliedAsDefault: Boolean,
        /** URI of the newly-written ringtone — used by PREVIEW. */
        val writtenUri: android.net.Uri? = null,
        /** Whether an UNDO action should be offered (set only if the overwrite captured a prior URI). */
        val undoAvailable: Boolean = false,
        /** Informative Snackbar subtitle, e.g. "Rick Astley 00:12-42". */
        val detail: String? = null,
        /** Optional inline warning appended to the success message. */
        val warning: String? = null
    ) : SaveResult()
    data class Error(val message: String) : SaveResult()
    /** User bailed out (e.g. cancelled the duplicate dialog). No snackbar. */
    object Cancelled : SaveResult()
}

@Composable
fun TrimScreen(
    vm: TrimViewModel,
    onSaveRequested: suspend (title: String, slot: RingtoneSlot, applyAsDefault: Boolean) -> SaveResult,
    onUndo: (suspend (RingtoneSlot) -> Unit)? = null,
    onPreview: ((android.net.Uri) -> Unit)? = null
) {
    val state by vm.state.collectAsState()
    val ctx = LocalContext.current
    val preview = remember { ExoPreviewController(ctx) }
    DisposableEffect(state.audioFile) {
        preview.load(state.audioFile, state.startMs, state.endMs)
        onDispose { preview.release() }
    }
    LaunchedEffect(state.selection) {
        preview.updateRange(state.startMs, state.endMs, state.audioFile)
    }
    var showSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Button(onClick = { showSheet = true }, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("벨소리로 설정 (${state.segmentMs / 1000}s)")
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Text(state.metadata.title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("전체 ${state.metadata.durationMs / 1000}s  |  선택 ${formatMmSs(state.startMs)} ~ ${formatMmSs(state.endMs)} (${state.segmentMs / 1000}s)")
            Spacer(Modifier.height(16.dp))
            WaveformCanvas(
                samples = state.samples,
                selection = state.selection,
                onSelectionChange = vm::updateSelection,
                totalDurationMs = state.metadata.durationMs
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { preview.togglePlay() }) { Text("재생/일시정지") }
                Spacer(Modifier.width(16.dp))
                FilterChip(selected = state.fadeEnabled, onClick = vm::toggleFade, label = { Text("페이드") })
                Spacer(Modifier.width(8.dp))
                FilterChip(selected = state.loopPreview, onClick = vm::toggleLoop, label = { Text("반복") })
            }
        }
    }
    if (showSheet) {
        SaveConfirmSheet(
            defaultTitle = state.metadata.title,
            onDismiss = { showSheet = false },
            onConfirm = { title, slot, applyDefault ->
                showSheet = false
                scope.launch {
                    val result = onSaveRequested(title, slot, applyDefault)
                    when (result) {
                        is SaveResult.Success -> showSuccessSnackbar(
                            host = snackbarHostState,
                            result = result,
                            onUndo = onUndo,
                            onPreview = onPreview,
                            scope = scope
                        )
                        is SaveResult.Error -> snackbarHostState.showSnackbar("저장 실패: ${result.message}")
                        SaveResult.Cancelled -> Unit
                    }
                }
            }
        )
    }
}

private fun formatMmSs(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}

/**
 * Informative post-save Snackbar. Material 3 action-carrying Snackbar supports
 * a single action label, so we prioritise UNDO when the overwrite captured a
 * prior URI; otherwise offer PREVIEW. 6-second duration per M3 spec.
 */
private suspend fun showSuccessSnackbar(
    host: SnackbarHostState,
    result: SaveResult.Success,
    onUndo: (suspend (RingtoneSlot) -> Unit)?,
    onPreview: ((android.net.Uri) -> Unit)?,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val base = if (result.appliedAsDefault) "✓ ${result.slot.koreanLabel}(으)로 설정"
    else "✓ ${result.slot.koreanLabel} 저장 완료"
    val detail = result.detail?.let { " · $it" } ?: ""
    val warn = result.warning?.let { " · $it" } ?: ""
    val message = "$base$detail$warn"

    val canUndo = result.undoAvailable && onUndo != null && result.appliedAsDefault
    val canPreview = result.writtenUri != null && onPreview != null

    val actionLabel = when {
        canUndo -> "UNDO"
        canPreview -> "PREVIEW"
        else -> null
    }

    val r = host.showSnackbar(
        message = message,
        actionLabel = actionLabel,
        duration = SnackbarDuration.Short,
        withDismissAction = true
    )
    if (r == SnackbarResult.ActionPerformed) {
        when {
            canUndo -> onUndo?.invoke(result.slot)
            canPreview -> result.writtenUri?.let { onPreview?.invoke(it) }
        }
        // If we handled UNDO, offer PREVIEW as a follow-up so the user can
        // still hear what they saved before it was reverted.
        if (canUndo && canPreview) {
            scope.launch {
                val second = host.showSnackbar(
                    message = "복원 완료. 저장된 벨소리를 미리 들어볼까요?",
                    actionLabel = "PREVIEW",
                    duration = SnackbarDuration.Short
                )
                if (second == SnackbarResult.ActionPerformed) {
                    result.writtenUri?.let { onPreview?.invoke(it) }
                }
            }
        }
    }
}

