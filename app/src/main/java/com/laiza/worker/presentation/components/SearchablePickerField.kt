package com.laiza.worker.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Smooth searchable picker: selected value + clear, open resets query so the
 * full list shows again (avoids getting stuck on the last selected name).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchablePickerField(
    selected: String,
    onSelected: (String) -> Unit,
    options: List<String>,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "Search…",
    emptyText: String = "No matches",
    optionSubtitle: ((String) -> String?)? = null,
    addNewLabel: String? = null,
    onAddNew: (() -> Unit)? = null,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(selected) { mutableStateOf("") }

    val filtered = remember(options, query, expanded) {
        if (!expanded || query.isBlank()) options
        else options.filter { it.contains(query, ignoreCase = true) }
    }

    val fieldValue = when {
        expanded -> query
        selected.isNotBlank() -> selected
        else -> ""
    }

    ExposedDropdownMenuBox(
        expanded = expanded && enabled,
        onExpandedChange = { wantOpen ->
            if (!enabled) return@ExposedDropdownMenuBox
            if (wantOpen) {
                query = ""
                expanded = true
            } else {
                expanded = false
                query = ""
            }
        },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { text ->
                if (!expanded) {
                    query = ""
                    expanded = true
                }
                query = text
                if (selected.isNotBlank() && !text.equals(selected, ignoreCase = true)) {
                    onSelected("")
                }
                expanded = true
            },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            enabled = enabled,
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selected.isNotBlank() && !expanded) {
                        IconButton(
                            onClick = {
                                onSelected("")
                                query = ""
                                expanded = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Clear",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded && enabled,
            onDismissRequest = {
                expanded = false
                query = ""
            },
            modifier = Modifier.heightIn(max = 280.dp)
        ) {
            if (filtered.isEmpty() && onAddNew == null) {
                DropdownMenuItem(
                    text = { Text(emptyText) },
                    onClick = {},
                    enabled = false
                )
            }
            filtered.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(option, fontWeight = FontWeight.Medium)
                            optionSubtitle?.invoke(option)?.let { sub ->
                                Text(
                                    sub,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelected(option)
                        query = ""
                        expanded = false
                    }
                )
            }
            if (onAddNew != null && !addNewLabel.isNullOrBlank()) {
                DropdownMenuItem(
                    text = { Text(addNewLabel) },
                    onClick = {
                        expanded = false
                        query = ""
                        onAddNew()
                    }
                )
            }
        }
    }
}

