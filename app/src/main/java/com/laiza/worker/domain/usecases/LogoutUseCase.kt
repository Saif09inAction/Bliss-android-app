package com.laiza.worker.domain.usecases

import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Resource<Unit>> {
        return authRepository.logout()
    }
}
