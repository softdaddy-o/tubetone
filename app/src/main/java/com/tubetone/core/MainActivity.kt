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
import com.tubetone.extract.ExtractionSource
import com.tubetone.extract.ExtractionState
import com.tubetone.library.db.RingtoneSource
import com.tubetone.library.LibraryScreen
import com.tubetone.library.LibraryViewModel
import com.tubetone.library.RingtoneRepository
import com.tubetone.library.db.RingtoneEntity
import com.tubetone.library.db.TubeToneDatabase
import com.tubetone.ringtone.PriorUriCache
import com.tubetone.ringtone.RingtoneSlot
import com.tubetone.ringtone.RingtoneWriter
import com.tubetone.ringtone.SlotPreferences
import com.tubetone.ringtone.SystemRingtoneApplier
import com.tubetone.share.YoutubeUrlParser
import com.tubetone.ui.theme.TubeToneTheme
import com.tubetone.trim.SaveResult
import com.tubetone.trim.TrimCoordinator
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
        setContent { TubeToneTheme { AppRoot(vm) } }
    }
}

@Composable
fun AppRoot(vm: ExtractionViewModel) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    val priorUriCache = remember { PriorUriCache() }
    val slotPrefs = remember { SlotPreferences(ctx) }

    Scaffold(bottomBar = {
        NavigationBar {
            NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
            NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.List, null) }, label = { Text("Library") })
        }
    }) { padding ->
        Box(Modifier.padding(padding)) {
            when (tab) {
                0 -> HomeTab(vm, ctx, priorUriCache, slotPrefs)
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
                                // v2: honour the slot the ringtone was saved into,
                                // not a hardcoded TYPE_RINGTONE. Fixes the v0.1.1
                                // bug where re-applying an alarm reset it to ringtone.
                                val slot = RingtoneSlot.fromTypeCode(item.slotType)
                                applier.setAsDefault(Uri.parse(item.outputUri), slot)
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
    ctx: android.content.Context,
    priorUriCache: PriorUriCache,
    slotPrefs: SlotPreferences
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
                    TrimUiState(
                        metadata = s.metadata,
                        audioFile = s.audioFile,
                        samples = s.waveform,
                        source = if (s.source == ExtractionSource.LOCAL) RingtoneSource.LOCAL else RingtoneSource.YOUTUBE
                    )
                ).also { it.initialThirtySecond() }
            }
            TrimScreen(
                vm = trimVm,
                initialSlot = slotPrefs.lastUsed(),
                occupantLabel = { slotToShow ->
                    // S6 — current occupant for the selected slot. Return null
                    // if the device has no default or RingtoneManager fails.
                    runCatching {
                        val applier = SystemRingtoneApplier(ctx)
                        val uri = applier.currentDefault(slotToShow) ?: return@runCatching null
                        val rt = android.media.RingtoneManager.getRingtone(ctx, uri)
                        rt?.getTitle(ctx)
                    }.getOrNull()
                },
                onSaveRequested = { title, slot, applyDefault ->
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
                                DupAction.Cancel -> return@TrimScreen SaveResult.Cancelled
                                DupAction.Overwrite -> dao.delete(dup.id)
                                DupAction.NewFile -> { /* proceed */ }
                            }
                        }
                        val output = File(ctx.cacheDir, "trimmed/${UUID.randomUUID()}.m4a").apply { parentFile?.mkdirs() }
                        TrimCoordinator.run(TrimParams(
                            inputPath = st.audioFile.absolutePath,
                            outputPath = output.absolutePath,
                            startMs = st.startMs,
                            endMs = st.endMs,
                            fade = st.fadeEnabled
                        ))
                        val written = RingtoneWriter(ctx).writeAsRingtone(output, title, slot)
                        val applier = SystemRingtoneApplier(ctx)
                        val canWrite = applier.canWriteSettings()
                        val appliedNow = applyDefault && canWrite
                        val entity = RingtoneEntity(
                            id = UUID.randomUUID().toString(),
                            title = title,
                            sourceUrl = if (st.source == RingtoneSource.LOCAL) "" else YoutubeUrlParser.canonicalUrl(st.metadata.videoId),
                            sourceVideoId = st.metadata.videoId,
                            sourceTitle = st.metadata.title,
                            thumbnailUrl = st.metadata.thumbnailUrl,
                            startMs = st.startMs, endMs = st.endMs, durationMs = st.segmentMs,
                            fadeEnabled = st.fadeEnabled,
                            outputUri = written.uri.toString(),
                            outputFilePath = written.filePath,
                            originalCachePath = st.audioFile.absolutePath,
                            createdAt = System.currentTimeMillis(),
                            lastAppliedAt = if (appliedNow) System.currentTimeMillis() else null,
                            slotType = slot.ringtoneManagerType,
                            source = st.source.name
                        )
                        RingtoneRepository(dao).save(entity)
                        // S5 — remember the slot the user just picked.
                        slotPrefs.setLastUsed(slot)
                        var permissionWarning: String? = null
                        var undoAvailable = false
                        if (applyDefault) {
                            if (canWrite) {
                                // Capture the currently-applied system URI for this slot BEFORE
                                // we overwrite it, so the UNDO action can restore.
                                val prior = runCatching { applier.currentDefault(slot) }.getOrNull()
                                priorUriCache.remember(slot, prior)
                                applier.setAsDefault(written.uri, slot)
                                undoAvailable = prior != null
                            } else {
                                applier.openWriteSettingsScreen()
                                permissionWarning = "권한을 허용하면 즉시 적용됩니다"
                            }
                        }
                        val detail = "${st.metadata.title.take(24)} ${formatMmSsShort(st.startMs)}-${formatMmSsShort(st.endMs)}"
                        SaveResult.Success(
                            slot = slot,
                            appliedAsDefault = appliedNow,
                            writtenUri = written.uri,
                            undoAvailable = undoAvailable && appliedNow,
                            detail = detail,
                            warning = permissionWarning
                        )
                    } catch (t: Throwable) {
                        SaveResult.Error(t.message ?: t::class.simpleName ?: "알 수 없는 오류")
                    }
                },
                onUndo = { slot ->
                    priorUriCache.take(slot)?.let { prev ->
                        val applier = SystemRingtoneApplier(ctx)
                        if (applier.canWriteSettings()) {
                            runCatching { applier.setAsDefault(prev, slot) }
                        }
                    }
                    Unit
                },
                onPreview = { uri ->
                    SystemRingtoneApplier(ctx).preview(uri)
                    Unit
                }
            )
        }
        ExtractionState.Idle -> HomeEntryScreen(
            onStart = { videoId -> ExtractionForegroundService.start(ctx, videoId) },
            onLocalFile = { uri ->
                // Persist permission for OpenDocument URIs so the ingestor can
                // re-read after a process death / service restart.
                runCatching {
                    ctx.contentResolver.takePersistableUriPermission(
                        uri,
                        android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                ExtractionForegroundService.startLocal(ctx, uri)
            }
        )
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

private fun formatMmSsShort(ms: Long): String {
    val s = ms / 1000
    return "%02d:%02d".format(s / 60, s % 60)
}

