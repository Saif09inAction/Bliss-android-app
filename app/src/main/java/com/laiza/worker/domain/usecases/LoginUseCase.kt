package com.laiza.worker.domain.usecases

import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.UserSession
import com.laiza.worker.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

import com.laiza.worker.domain.models.Role

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(phone: String, password: String, role: Role): Flow<Resource<UserSession>> {
        val trimmedPhone = phone.trim()
        val trimmedPassword = password.trim()

        if (trimmedPhone.isEmpty()) {
            return flow { emit(Resource.Error("Mobile number cannot be empty")) }
        }
        if (trimmedPassword.isEmpty()) {
            return flow { emit(Resource.Error("Password cannot be empty")) }
        }

        return authRepository.login(trimmedPhone, trimmedPassword, role)
    }
}
