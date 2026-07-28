package com.laiza.worker.core.network

import com.laiza.worker.core.session.DataStoreManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class AuthInterceptor @Inject constructor(
    private val dataStoreManagerProvider: Provider<DataStoreManager>
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val dataStoreManager = dataStoreManagerProvider.get()
        val originalRequest = chain.request()
        
        // Read token in a blocking way since interceptor runs on a background thread
        val token = runBlocking {
            dataStoreManager.jwtToken.firstOrNull()
        }

        val requestBuilder = originalRequest.newBuilder()
        if (!token.isNullOrBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $token")
        }

        var response = chain.proceed(requestBuilder.build())

        // Handle token refresh on 401 Unauthorized
        if (response.code == 401) {
            synchronized(this) {
                // Re-fetch current token to check if another request refreshed it already
                val currentToken = runBlocking { dataStoreManager.jwtToken.firstOrNull() }
                
                if (currentToken == token) {
                    val refreshToken = runBlocking { dataStoreManager.refreshToken.firstOrNull() }
                    
                    if (!refreshToken.isNullOrBlank()) {
                        val newTokens = refreshAccessToken(refreshToken)
                        if (newTokens != null) {
                            runBlocking {
                                dataStoreManager.saveJwtToken(newTokens.first)
                                dataStoreManager.saveRefreshToken(newTokens.second)
                            }
                            
                            // Retry the request with new token
                            response.close()
                            val newRequest = originalRequest.newBuilder()
                                .addHeader("Authorization", "Bearer ${newTokens.first}")
                                .build()
                            response = chain.proceed(newRequest)
                        } else {
                            // Refresh failed, clear session to force logout
                            runBlocking {
                                dataStoreManager.clearSession()
                            }
                        }
                    } else {
                        // No refresh token, clear session
                        runBlocking {
                            dataStoreManager.clearSession()
                        }
                    }
                } else {
                    // Token was already refreshed by another thread, retry request with new token
                    if (!currentToken.isNullOrBlank()) {
                        response.close()
                        val newRequest = originalRequest.newBuilder()
                            .addHeader("Authorization", "Bearer $currentToken")
                            .build()
                        response = chain.proceed(newRequest)
                    }
                }
            }
        }

        return response
    }

    /**
     * Synchronously calls the backend to refresh the JWT using the refresh token.
     * Returns Pair(NewAccessToken, NewRefreshToken) or null if failed.
     */
    private fun refreshAccessToken(refreshToken: String): Pair<String, String>? {
        // In a real application, you would invoke a synchronous OkHttp request to your auth endpoint.
        // E.g., val request = Request.Builder().url(".../refresh").post(...).build()
        // For foundation, we return null since we do not have an active backend endpoint yet.
        return null
    }
}
