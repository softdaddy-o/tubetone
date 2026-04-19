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
import com.tubetone.trim.SaveResult
import com.tubetone.trim.TrimParams
import com.tubetone.trim.TrimScreen
import com.tubetone.trim.TrimUiState
import com.tubetone.trim.TrimViewModel
import kotlinx.coroutines.flow.StateFlow
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
    var tab by remember { mutableStateOf(0) }

    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.List, null) }, label = { Text("Library") })
        }
    }) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeTab(vm, ctx)
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

private enum class DupAction { Overwrite, NewFile, Cancel }

@Composable
private fun HomeTab(
    vm: ExtractionViewModel,
    ctx: android.content.Context
) {
    val state by vm.state.collectAsState()
    var pendingDup by remember { mutableStateOf<((DupAction) -> Unit)?>(null) }

    pendingDup?.let { resume ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { resume(DupAction.Cancel); pendingDup = null },
            title = { Text("중복된 벨소리") },
            text = { Text("같은 영상의 같은 구간이 이미 저장되어 있습니다.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { resume(DupAction.Overwrite); pendingDup = null }) { Text("덮어쓰기") }
            },
            dismissButton = {
                androidx.compose.foundation.layout.Row {
                    androidx.compose.material3.TextButton(onClick = { resume(DupAction.NewFile); pendingDup = null }) { Text("새 파일") }
                    androidx.compose.material3.TextButton(onClick = { resume(DupAction.Cancel); pendingDup = null }) { Text("취소") }
                }
            }
        )
    }

    val s = state
    when (s) {
        is ExtractionState.Ready -> {
            val trimVm = remember(s.audioFile) {
                TrimViewModel(
                    TrimUiState(metadata = s.metadata, audioFile = s.audioFile, samples = s.waveform)
                ).also { it.initialThirtySecond() }
            }
            TrimScreen(vm = trimVm, onSaveRequested = { title, applyDefault ->
                try {
                    val st = trimVm.state.value
                    val dao = TubeToneDatabase.get(ctx).ringtoneDao()
                    val dup = dao.findDuplicate(st.metadata.videoId, st.startMs, st.endMs)
                    if (dup != null) {
                        val action = kotlinx.coroutines.suspendCancellableCoroutine<DupAction> { cont ->
                            pendingDup = { a -> if (cont.isActive) cont.resumeWith(Result.success(a)) }
                            cont.invokeOnCancellation { pendingDup = null }
                        }
                        when (action) {
                            DupAction.Cancel -> return@TrimScreen SaveResult.Error("취소되었습니다")
                            DupAction.Overwrite -> dao.delete(dup.id)
                            DupAction.NewFile -> { /* proceed */ }
                        }
                    }
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
                    val canWrite = applier.canWriteSettings()
                    val appliedNow = applyDefault && canWrite
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
                        lastAppliedAt = if (appliedNow) System.currentTimeMillis() else null
                    )
                    RingtoneRepository(dao).save(entity)
                    if (applyDefault) {
                        if (canWrite) applier.setAsDefaultRingtone(written.uri)
                        else {
                            applier.openWriteSettingsScreen()
                            return@TrimScreen SaveResult.Error("설정 쓰기 권한을 허용해 주세요")
                        }
                    }
                    SaveResult.Success(appliedAsDefault = appliedNow)
                } catch (t: Throwable) {
                    SaveResult.Error(t.message ?: t::class.simpleName ?: "알 수 없는 오류")
                }
            })
        }
        ExtractionState.Idle -> HomeScreen()
        else -> ExtractingScreen(
            state = s,
            onCancel = vm::cancel,
            onRetry = {
                val vid = when (val cur = s) {
                    is ExtractionState.FetchingMetadata -> cur.videoId
                    is ExtractionState.Downloading -> cur.metadata.videoId
                    is ExtractionState.AnalyzingWaveform -> cur.metadata.videoId
                    else -> null
                }
                if (vid != null) ExtractionForegroundService.start(ctx, vid)
            }
        )
    }
}

@Composable
fun HomeScreen() {
    Scaffold { padding ->
        Text("TubeTone", modifier = Modifier.padding(padding.calculateTopPadding()).padding(16.dp))
    }
}
