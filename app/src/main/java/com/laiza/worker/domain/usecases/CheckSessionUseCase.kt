package com.laiza.worker.domain.usecases

import com.laiza.worker.domain.models.UserSession
import com.laiza.worker.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CheckSessionUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<UserSession?> {
        return authRepository.getCurrentSession()
    }
}
