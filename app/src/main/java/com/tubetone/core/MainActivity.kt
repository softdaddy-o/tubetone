package com.tubetone.core

import android.app.Application
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import com.tubetone.extract.ExtractingScreen
import com.tubetone.extract.ExtractionForegroundService
import com.tubetone.extract.ExtractionState
import com.tubetone.trim.TrimScreen
import com.tubetone.trim.TrimUiState
import com.tubetone.trim.TrimViewModel
import kotlinx.coroutines.flow.StateFlow

class ExtractionViewModel(app: Application) : AndroidViewModel(app) {
    val state: StateFlow<ExtractionState> = ExtractionForegroundService.state
    fun cancel() {
        getApplication<Application>().stopService(
            Intent(getApplication(), ExtractionForegroundService::class.java)
        )
    }
}

class MainActivity : ComponentActivity() {
    private val vm: ExtractionViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val state by vm.state.collectAsState()
            val s = state
            when (s) {
                is ExtractionState.Ready -> {
                    val trimVm = remember(s.audioFile) {
                        TrimViewModel(
                            TrimUiState(metadata = s.metadata, audioFile = s.audioFile, samples = s.waveform)
                        ).also { it.initialThirtySecond() }
                    }
                    TrimScreen(vm = trimVm, onSaveRequested = { /* Milestone 5 wires */ })
                }
                ExtractionState.Idle -> HomeScreen()
                else -> ExtractingScreen(state = s, onCancel = vm::cancel)
            }
        }
    }
}

@Composable
fun HomeScreen() {
    Scaffold { padding ->
        Text("TubeTone", modifier = Modifier.padding(padding.calculateTopPadding()).padding(16.dp))
    }
}
