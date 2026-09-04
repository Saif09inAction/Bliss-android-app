package com.laiza.worker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.laiza.worker.R

@Composable
fun KaarigerLanguageSwitch(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.kaariger_language),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selectedLanguage == "en",
                onClick = { onLanguageSelected("en") },
                label = { Text(stringResource(R.string.kaariger_english)) },
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = selectedLanguage == "hi",
                onClick = { onLanguageSelected("hi") },
                label = { Text(stringResource(R.string.kaariger_hindi)) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
