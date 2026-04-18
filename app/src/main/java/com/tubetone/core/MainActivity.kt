package com.tubetone.core

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import com.tubetone.extract.ExtractingScreen
import com.tubetone.extract.ExtractionState
import kotlinx.coroutines.flow.MutableStateFlow

class ExtractionViewModel : ViewModel() {
    val state = MutableStateFlow<ExtractionState>(ExtractionState.Idle)
    fun cancel() { /* Milestone 2 fills this */ }
}

class MainActivity : ComponentActivity() {
    private val vm: ExtractionViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val videoId = intent?.getStringExtra("videoId")
        if (videoId != null) vm.state.value = ExtractionState.FetchingMetadata(videoId)
        setContent {
            val state by vm.state.collectAsState()
            when (state) {
                is ExtractionState.Idle -> HomeScreen()
                else -> ExtractingScreen(state = state, onCancel = vm::cancel)
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
