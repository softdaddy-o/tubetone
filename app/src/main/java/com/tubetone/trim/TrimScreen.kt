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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
    data class Success(val slot: RingtoneSlot, val appliedAsDefault: Boolean) : SaveResult()
    data class Error(val message: String) : SaveResult()
}

@Composable
fun TrimScreen(
    vm: TrimViewModel,
    onSaveRequested: suspend (title: String, slot: RingtoneSlot, applyAsDefault: Boolean) -> SaveResult
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
                    val message = when (result) {
                        is SaveResult.Success ->
                            if (result.appliedAsDefault) "✓ ${result.slot.koreanLabel}(으)로 설정되었습니다"
                            else "✓ ${result.slot.koreanLabel} 저장 완료"
                        is SaveResult.Error -> "저장 실패: ${result.message}"
                    }
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }
}

private fun formatMmSs(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}
