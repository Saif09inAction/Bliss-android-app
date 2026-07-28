package com.laiza.worker.core.session

import com.google.gson.Gson
import com.laiza.worker.domain.models.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val dataStoreManager: DataStoreManager,
    private val gson: Gson
) {
    /**
     * Emits the current user session. Emits null if the user is logged out.
     */
    val userSession: Flow<UserSession?> = dataStoreManager.loggedUser.map { json ->
        if (json.isNullOrBlank()) {
            null
        } else {
            try {
                gson.fromJson(json, UserSession::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Starts a new user session by saving the tokens and the session profile.
     */
    suspend fun startSession(session: UserSession) {
        val json = gson.toJson(session)
        dataStoreManager.saveLoggedUser(json)
        dataStoreManager.saveJwtToken(session.token ?: "")
    }

    /**
     * Ends the active user session.
     */
    suspend fun endSession() {
        dataStoreManager.clearSession()
    }

    /**
     * Checks if a session is currently active (i.e. user is logged in).
     */
    suspend fun isSessionActive(): Boolean {
        return userSession.map { it != null }.firstOrNull() ?: false
    }

    /**
     * Helper to get the active JWT token directly in a blocking interceptor context.
     */
    suspend fun getJwtToken(): String? {
        return dataStoreManager.jwtToken.firstOrNull()
    }
}
