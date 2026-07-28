package com.laiza.worker.core.network

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object NetworkErrorHandler {

    fun getErrorMessage(throwable: Throwable): String {
        return when (throwable) {
            is UnknownHostException -> "No internet connection. Please check your network settings."
            is IOException -> "Network error occurred. Please try again."
            is SocketTimeoutException -> "Request timed out. Please try again later."
            is HttpException -> {
                when (throwable.code()) {
                    400 -> "Invalid request. Please check input parameters."
                    401 -> "Unauthorized access. Please login again."
                    403 -> "Access denied. You do not have permissions for this action."
                    404 -> "Requested resource not found on server."
                    408 -> "Request timeout. Server took too long to respond."
                    500 -> "Internal server error. Please try again later."
                    503 -> "Server is temporarily unavailable. Please try again later."
                    else -> "An unexpected server error occurred (${throwable.code()})."
                }
            }
            else -> throwable.localizedMessage ?: "An unknown error occurred."
        }
    }
}
