package com.laiza.worker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.laiza.worker.core.session.SessionManager
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.Role
import com.laiza.worker.domain.models.UserSession
import com.laiza.worker.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager
) : AuthRepository {

    override fun login(phone: String, password: String, role: Role): Flow<Resource<UserSession>> = flow {
        emit(Resource.Loading())
        try {
            val trimPhone = phone.trim()
            val data = getEmployeeFromFirestore(trimPhone)
            if (data == null) {
                emit(Resource.Error("Profile not found. Please contact admin."))
                return@flow
            }
            val dbPassword = data["password"] as? String ?: "123123"
            if (dbPassword != password) {
                emit(Resource.Error("Incorrect credentials"))
                return@flow
            }
            val storedRole = Role.fromFirestore(data["role"] as? String)
            if (storedRole != role) {
                emit(Resource.Error("This account is registered as ${storedRole.name.lowercase()}. Please use the correct login tab."))
                return@flow
            }
            val name = data["name"] as? String ?: "${role.name} $trimPhone"
            val userSession = UserSession(
                uid = trimPhone,
                name = name,
                phone = trimPhone,
                role = storedRole,
                token = "${storedRole.name.lowercase()}_token"
            )
            sessionManager.startSession(userSession)
            emit(Resource.Success(userSession))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Login failed"))
        }
    }

    override fun logout(): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            sessionManager.endSession()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Logout failed"))
        }
    }

    override fun getCurrentSession(): Flow<UserSession?> {
        return sessionManager.userSession
    }

    private suspend fun getEmployeeFromFirestore(phone: String): Map<String, Any>? = suspendCancellableCoroutine { continuation ->
        firestore.collection("employees").document(phone).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    continuation.resume(doc.data)
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener { err ->
                continuation.resumeWithException(err)
            }
    }
}
