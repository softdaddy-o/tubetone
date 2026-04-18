package com.tubetone.trim

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveConfirmSheet(
    defaultTitle: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, applyAsDefault: Boolean) -> Unit
) {
    var title by remember { mutableStateOf(defaultTitle.take(30)) }
    var applyDefault by remember { mutableStateOf(true) }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp)) {
            Text("벨소리 저장", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(value = title, onValueChange = { title = it.take(60) }, label = { Text("파일명") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = applyDefault, onCheckedChange = { applyDefault = it })
                Text("기본 벨소리로 즉시 적용")
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = { onConfirm(title, applyDefault) }, modifier = Modifier.fillMaxWidth()) { Text("저장") }
        }
    }
}
