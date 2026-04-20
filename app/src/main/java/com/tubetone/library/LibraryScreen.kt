package com.tubetone.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tubetone.library.db.RingtoneEntity
import com.tubetone.library.db.RingtoneSource
import com.tubetone.ringtone.RingtoneSlot
import com.tubetone.ui.theme.PillShape
import com.tubetone.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(items: List<RingtoneEntity>, onApply: (RingtoneEntity) -> Unit, onDelete: (RingtoneEntity) -> Unit) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("저장된 벨소리가 없습니다", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = Spacing.s16)) {
        items(items, key = { it.id }) { item ->
            var showSheet by remember { mutableStateOf(false) }
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = Spacing.s4).clickable { showSheet = true },
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(Spacing.s16)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(Spacing.s4))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.s8)) {
                        Text(
                            text = "${item.durationMs / 1000}s",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "·  ${RingtoneSlot.fromTypeCode(item.slotType).koreanLabel}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.weight(1f))
                        SourcePill(item.source)
                    }
                    Text(
                        text = item.sourceTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1
                    )
                }
            }
            if (showSheet) {
                ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                    Column(Modifier.padding(Spacing.s24)) {
                        Button(onClick = { onApply(item); showSheet = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("${RingtoneSlot.fromTypeCode(item.slotType).koreanLabel}(으)로 적용")
                        }
                        Spacer(Modifier.height(Spacing.s8))
                        OutlinedButton(onClick = { onDelete(item); showSheet = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("삭제")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourcePill(rawSource: String) {
    val src = RingtoneSource.parse(rawSource)
    val (label, bg, fg) = when (src) {
        RingtoneSource.YOUTUBE -> Triple("YT", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
        RingtoneSource.LOCAL -> Triple("파일", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurface)
    }
    Surface(shape = PillShape, color = bg, contentColor = fg) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
        )
    }
}

// Unused Color import guard
@Suppress("unused") private val unusedColor = Color.Transparent
