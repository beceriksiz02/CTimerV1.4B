package com.premium.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddTimerDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, totalMillis: Long, project: String?, tags: String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var hours by remember { mutableStateOf("0") }
    var minutes by remember { mutableStateOf("5") }
    var seconds by remember { mutableStateOf("0") }
    var project by remember { mutableStateOf("") }
    var tags by remember { mutableStateOf("") }
    var showAdvanced by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New timer") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Hours", hours) { hours = it }
                    NumberField("Min", minutes) { minutes = it }
                    NumberField("Sec", seconds) { seconds = it }
                }

                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(if (showAdvanced) "Hide project/tags" else "Add project or tags")
                }
                if (showAdvanced) {
                    OutlinedTextField(
                        value = project,
                        onValueChange = { project = it },
                        label = { Text("Project (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = tags,
                        onValueChange = { tags = it },
                        label = { Text("Tags, comma-separated (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val h = hours.toLongOrNull() ?: 0L
                val m = minutes.toLongOrNull() ?: 0L
                val s = seconds.toLongOrNull() ?: 0L
                val total = ((h * 3600) + (m * 60) + s) * 1000
                if (total > 0) {
                    val label = name.ifBlank { formatHms(total) }
                    onConfirm(label, total, project.ifBlank { null }, tags.ifBlank { null })
                }
            }) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RowScope.NumberField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { new -> if (new.length <= 3 && new.all { it.isDigit() }) onChange(new) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.weight(1f)
    )
}
