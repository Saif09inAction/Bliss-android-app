package com.laiza.worker.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.laiza.worker.core.utils.LocaleHelper

@Composable
fun KaarigerLocalizedContent(
    languageCode: String,
    content: @Composable () -> Unit
) {
    val baseContext = LocalContext.current
    val localizedContext = remember(languageCode) {
        LocaleHelper.withLocale(baseContext, languageCode)
    }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalContext provides localizedContext
    ) {
        content()
    }
}
