package com.laiza.worker.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavController
import com.laiza.worker.core.navigation.Screen
import com.laiza.worker.domain.models.UserSession

/**
 * Redirects to login only after an active session was lost (logout),
 * avoiding a race where DataStore has not emitted yet on first load.
 */
@Composable
fun SessionGuard(
    session: UserSession?,
    rootNavController: NavController
) {
    var hadSession by remember { mutableStateOf(false) }

    LaunchedEffect(session) {
        if (session != null) {
            hadSession = true
        } else if (hadSession) {
            rootNavController.navigate(Screen.AuthGraph.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}
