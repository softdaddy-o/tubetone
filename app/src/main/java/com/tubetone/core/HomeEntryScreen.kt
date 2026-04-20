package com.tubetone.core

import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.tubetone.share.YoutubeUrlParser
import com.tubetone.ui.theme.Spacing

/**
 * Landing screen shown when the app is launched without a share intent
 * and the ExtractionState is Idle. Two primary CTAs:
 *  - paste YouTube URL → existing extraction pipeline.
 *  - pick a local audio/video file → L1 local path (v0.2.0 MUST).
 */
@Composable
fun HomeEntryScreen(
    onStart: (videoId: String) -> Unit,
    onLocalFile: (Uri) -> Unit = {}
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var url by remember { mutableStateOf("") }
    var invalid by remember { mutableStateOf(false) }
    var clipboardUrl by remember { mutableStateOf<String?>(null) }

    // Re-check the clipboard every time the user returns to this screen.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                clipboardUrl = readYoutubeUrlFromClipboard(ctx)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // OpenDocument contract with audio+video MIME filter — scoped-storage
    // friendly, no READ_MEDIA_* permission required.
    val pickFile = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> if (uri != null) onLocalFile(uri) }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(Spacing.s24)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Text("TubeTone", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(Spacing.s8))
            Text(
                "유튜브 링크를 붙여넣거나 기기 파일을 선택해 벨소리를 만들어 보세요.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(Spacing.s24))

            clipboardUrl?.let { detected ->
                AssistChip(
                    onClick = {
                        url = detected
                        invalid = false
                    },
                    label = {
                        Text(
                            text = "클립보드의 URL 사용: ${truncateMiddle(detected, 40)}",
                            maxLines = 1
                        )
                    }
                )
                Spacer(Modifier.height(Spacing.s12))
            }

            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    invalid = false
                },
                label = { Text("유튜브 URL") },
                isError = invalid,
                supportingText = if (invalid) {
                    { Text("유튜브 URL을 인식하지 못했습니다") }
                } else null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(Spacing.s16))
            Button(
                onClick = {
                    val videoId = YoutubeUrlParser.extractVideoId(url)
                    if (videoId == null) {
                        invalid = true
                    } else {
                        onStart(videoId)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = url.isNotBlank()
            ) { Text("유튜브로 만들기") }
            Spacer(Modifier.height(Spacing.s12))
            OutlinedButton(
                onClick = { pickFile.launch(arrayOf("audio/*", "video/*")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("기기 파일에서 만들기") }
        }
    }
}

/**
 * Read the primary clip and return the first YouTube URL-like substring we
 * recognise, or null. Safe to call when clipboard permission is denied.
 */
internal fun readYoutubeUrlFromClipboard(ctx: Context): String? {
    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = runCatching { cm.primaryClip }.getOrNull() ?: return null
    for (i in 0 until clip.itemCount) {
        val text = clip.getItemAt(i).coerceToText(ctx)?.toString() ?: continue
        if (YoutubeUrlParser.extractVideoId(text) != null) {
            return text.trim()
        }
    }
    return null
}

internal fun truncateMiddle(s: String, max: Int): String {
    if (s.length <= max) return s
    val keep = (max - 3) / 2
    return s.take(keep) + "..." + s.takeLast(max - 3 - keep)
}
