package com.samco.trackandgraph.functions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.ui.cardPadding

@Composable
fun MockNode() = Column(
    Modifier.padding(horizontal = connectorSize / 2, vertical = cardPadding),
    verticalArrangement = Arrangement.spacedBy(8.dp)
) {
    Text("Hello", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(value = "", onValueChange = {}, label = { Text("Name") })
    DropdownMenuDemo()
}

@Composable
private fun DropdownMenuDemo() {
    var expanded by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf("Option A") }
    Column {
        Button(onClick = { expanded = true }) { Text(selected) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf("Option A", "Option B", "Option C").forEach {
                DropdownMenuItem(text = { Text(it) }, onClick = { selected = it; expanded = false })
            }
        }
    }
}

