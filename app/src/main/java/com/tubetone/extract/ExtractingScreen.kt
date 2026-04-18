package com.tubetone.extract

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
fun ExtractingScreen(state: ExtractionState, onCancel: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val (title, progress) = when (state) {
                is ExtractionState.FetchingMetadata -> "정보 가져오는 중..." to null
                is ExtractionState.Downloading -> state.metadata.title to state.progress
                is ExtractionState.AnalyzingWaveform -> state.metadata.title to null
                is ExtractionState.Failed -> "실패: ${state.reason}" to null
                is ExtractionState.Idle, is ExtractionState.Ready -> "" to null
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(24.dp))
            if (progress != null) LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
            else CircularProgressIndicator()
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onCancel) { Text("취소") }
        }
    }
}
