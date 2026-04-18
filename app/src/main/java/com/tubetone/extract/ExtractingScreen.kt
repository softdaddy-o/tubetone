package com.tubetone.extract

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ExtractingScreen(state: ExtractionState, onCancel: () -> Unit, onRetry: () -> Unit = {}) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (state) {
                is ExtractionState.FetchingMetadata -> {
                    Text("정보 가져오는 중...", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(24.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = onCancel) { Text("취소") }
                }
                is ExtractionState.Downloading -> {
                    Text(state.metadata.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(24.dp))
                    LinearProgressIndicator(progress = state.progress, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = onCancel) { Text("취소") }
                }
                is ExtractionState.AnalyzingWaveform -> {
                    Text(state.metadata.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(24.dp))
                    CircularProgressIndicator()
                    Spacer(Modifier.height(24.dp))
                    OutlinedButton(onClick = onCancel) { Text("취소") }
                }
                is ExtractionState.Failed -> {
                    val (msg, canRetry) = when (state.reason) {
                        FailureReason.NETWORK -> "네트워크 오류. 다시 시도해주세요." to true
                        FailureReason.EXTRACTOR_BROKEN -> "YouTube 호환이 깨졌습니다. NewPipeExtractor 업데이트 후 다시 시도." to false
                        FailureReason.TOO_LONG -> "영상이 30분을 초과합니다." to false
                        FailureReason.CANCELLED -> "취소됨" to true
                        FailureReason.UNKNOWN -> "알 수 없는 오류: ${state.cause?.message}" to true
                    }
                    Text(msg, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(16.dp))
                    Row {
                        if (canRetry) Button(onClick = onRetry) { Text("다시 시도") }
                        Spacer(Modifier.width(8.dp))
                        OutlinedButton(onClick = onCancel) { Text("닫기") }
                    }
                }
                is ExtractionState.Idle, is ExtractionState.Ready -> {}
            }
        }
    }
}
