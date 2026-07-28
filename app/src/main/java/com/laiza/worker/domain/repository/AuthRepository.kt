package com.laiza.worker.domain.repository

import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.UserSession
import kotlinx.coroutines.flow.Flow

import com.laiza.worker.domain.models.Role

interface AuthRepository {
    fun login(phone: String, password: String, role: Role): Flow<Resource<UserSession>>
    fun logout(): Flow<Resource<Unit>>
    fun getCurrentSession(): Flow<UserSession?>
}
