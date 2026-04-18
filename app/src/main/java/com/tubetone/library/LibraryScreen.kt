package com.tubetone.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tubetone.library.db.RingtoneEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(items: List<RingtoneEntity>, onApply: (RingtoneEntity) -> Unit, onDelete: (RingtoneEntity) -> Unit) {
    if (items.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("저장된 벨소리가 없습니다")
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        items(items, key = { it.id }) { item ->
            var showSheet by remember { mutableStateOf(false) }
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { showSheet = true }) {
                Column(Modifier.padding(16.dp)) {
                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                    Text("${item.durationMs / 1000}s  |  ${item.sourceTitle}", style = MaterialTheme.typography.bodySmall)
                }
            }
            if (showSheet) {
                ModalBottomSheet(onDismissRequest = { showSheet = false }) {
                    Column(Modifier.padding(24.dp)) {
                        Button(onClick = { onApply(item); showSheet = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("기본 벨소리로 설정")
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { onDelete(item); showSheet = false }, modifier = Modifier.fillMaxWidth()) {
                            Text("삭제")
                        }
                    }
                }
            }
        }
    }
}
