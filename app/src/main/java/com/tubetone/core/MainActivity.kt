package com.tubetone.core

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tubetone.extract.ExtractingScreen
import com.tubetone.extract.ExtractionForegroundService
import com.tubetone.extract.ExtractionState
import com.tubetone.library.LibraryScreen
import com.tubetone.library.LibraryViewModel
import com.tubetone.library.RingtoneRepository
import com.tubetone.library.db.RingtoneEntity
import com.tubetone.library.db.TubeToneDatabase
import com.tubetone.ringtone.RingtoneWriter
import com.tubetone.ringtone.SystemRingtoneApplier
import com.tubetone.share.YoutubeUrlParser
import com.tubetone.trim.MediaTrimmer
import com.tubetone.trim.TrimParams
import com.tubetone.trim.TrimScreen
import com.tubetone.trim.TrimUiState
import com.tubetone.trim.TrimViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

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
        setContent { AppRoot(vm) }
    }
}

@Composable
fun AppRoot(vm: ExtractionViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) }

    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.List, null) }, label = { Text("Library") })
        }
    }) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeTab(vm, ctx, scope)
                1 -> {
                    val libVm: LibraryViewModel = viewModel(factory = viewModelFactory {
                        initializer { LibraryViewModel(RingtoneRepository(TubeToneDatabase.get(ctx).ringtoneDao())) }
                    })
                    val libItems by libVm.items.collectAsState()
                    LibraryScreen(
                        items = libItems,
                        onApply = { item ->
                            val applier = SystemRingtoneApplier(ctx)
                            if (applier.canWriteSettings()) {
                                applier.setAsDefaultRingtone(Uri.parse(item.outputUri))
                                libVm.markApplied(item.id)
                            } else applier.openWriteSettingsScreen()
                        },
                        onDelete = { libVm.delete(it.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeTab(
    vm: ExtractionViewModel,
    ctx: android.content.Context,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val state by vm.state.collectAsState()
    val s = state
    when (s) {
        is ExtractionState.Ready -> {
            val trimVm = remember(s.audioFile) {
                TrimViewModel(
                    TrimUiState(metadata = s.metadata, audioFile = s.audioFile, samples = s.waveform)
                ).also { it.initialThirtySecond() }
            }
            TrimScreen(vm = trimVm, onSaveRequested = { title, applyDefault ->
                scope.launch {
                    val st = trimVm.state.value
                    val output = File(ctx.cacheDir, "trimmed/${UUID.randomUUID()}.m4a").apply { parentFile?.mkdirs() }
                    MediaTrimmer.trim(TrimParams(
                        inputPath = st.audioFile.absolutePath,
                        outputPath = output.absolutePath,
                        startMs = st.startMs,
                        endMs = st.endMs,
                        fade = st.fadeEnabled
                    ))
                    val written = RingtoneWriter(ctx).writeAsRingtone(output, title)
                    val applier = SystemRingtoneApplier(ctx)
                    val entity = RingtoneEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        sourceUrl = YoutubeUrlParser.canonicalUrl(st.metadata.videoId),
                        sourceVideoId = st.metadata.videoId,
                        sourceTitle = st.metadata.title,
                        thumbnailUrl = st.metadata.thumbnailUrl,
                        startMs = st.startMs, endMs = st.endMs, durationMs = st.segmentMs,
                        fadeEnabled = st.fadeEnabled,
                        outputUri = written.uri.toString(),
                        outputFilePath = written.filePath,
                        originalCachePath = st.audioFile.absolutePath,
                        createdAt = System.currentTimeMillis(),
                        lastAppliedAt = if (applyDefault && applier.canWriteSettings()) System.currentTimeMillis() else null
                    )
                    RingtoneRepository(TubeToneDatabase.get(ctx).ringtoneDao()).save(entity)
                    if (applyDefault) {
                        if (applier.canWriteSettings()) applier.setAsDefaultRingtone(written.uri)
                        else applier.openWriteSettingsScreen()
                    }
                }
            })
        }
        ExtractionState.Idle -> HomeScreen()
        else -> ExtractingScreen(state = s, onCancel = vm::cancel)
    }
}

@Composable
fun HomeScreen() {
    Scaffold { padding ->
        Text("TubeTone", modifier = Modifier.padding(padding.calculateTopPadding()).padding(16.dp))
    }
}
