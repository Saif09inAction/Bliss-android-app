package com.laiza.worker.core.session

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "laiza_worker_prefs")

@Singleton
class DataStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val JWT_TOKEN = stringPreferencesKey("jwt_token")
        private val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        private val LOGGED_USER = stringPreferencesKey("logged_user")
        private val APP_THEME = stringPreferencesKey("app_theme")
        private val APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KAARIGER_LANGUAGE = stringPreferencesKey("kaariger_language")
    }

    // JWT Token Flow
    val jwtToken: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[JWT_TOKEN]
        }

    suspend fun saveJwtToken(token: String) {
        dataStore.edit { preferences ->
            preferences[JWT_TOKEN] = token
        }
    }

    suspend fun clearJwtToken() {
        dataStore.edit { preferences ->
            preferences.remove(JWT_TOKEN)
        }
    }

    // Refresh Token Flow
    val refreshToken: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[REFRESH_TOKEN]
        }

    suspend fun saveRefreshToken(token: String) {
        dataStore.edit { preferences ->
            preferences[REFRESH_TOKEN] = token
        }
    }

    suspend fun clearRefreshToken() {
        dataStore.edit { preferences ->
            preferences.remove(REFRESH_TOKEN)
        }
    }

    // Logged User Flow (JSON String)
    val loggedUser: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[LOGGED_USER]
        }

    suspend fun saveLoggedUser(userJson: String) {
        dataStore.edit { preferences ->
            preferences[LOGGED_USER] = userJson
        }
    }

    suspend fun clearLoggedUser() {
        dataStore.edit { preferences ->
            preferences.remove(LOGGED_USER)
        }
    }

    // App Theme Flow
    val appTheme: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[APP_THEME] ?: "System"
        }

    suspend fun saveAppTheme(theme: String) {
        dataStore.edit { preferences ->
            preferences[APP_THEME] = theme
        }
    }

    // App Language Flow
    val appLanguage: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[APP_LANGUAGE] ?: "en"
        }

    suspend fun saveAppLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[APP_LANGUAGE] = language
        }
    }

    val kaarigerLanguage: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            preferences[KAARIGER_LANGUAGE] ?: "hi"
        }

    suspend fun saveKaarigerLanguage(language: String) {
        dataStore.edit { preferences ->
            preferences[KAARIGER_LANGUAGE] = language
        }
    }

    // Clear all session details (for Logout)
    suspend fun clearSession() {
        dataStore.edit { preferences ->
            preferences.remove(JWT_TOKEN)
            preferences.remove(REFRESH_TOKEN)
            preferences.remove(LOGGED_USER)
        }
    }
}
