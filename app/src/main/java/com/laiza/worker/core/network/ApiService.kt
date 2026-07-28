package com.laiza.worker.core.network

import retrofit2.http.GET

interface ApiService {
    @GET("api/v1/health")
    suspend fun checkHealth(): HealthCheckResponse

    data class HealthCheckResponse(
        val status: String,
        val version: String
    )
}
