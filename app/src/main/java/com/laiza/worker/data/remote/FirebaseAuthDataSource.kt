package com.laiza.worker.data.remote

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class FirebaseAuthDataSource @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    /**
     * Authenticates with Firebase using Email + Password and returns the ID Token.
     */
    suspend fun signIn(email: String, password: String): String = suspendCancellableCoroutine { continuation ->
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        user.getIdToken(false).addOnCompleteListener { tokenTask ->
                            if (tokenTask.isSuccessful) {
                                val token = tokenTask.result?.token
                                if (token != null) {
                                    continuation.resume(token)
                                } else {
                                    continuation.resumeWithException(Exception("Firebase ID token was null"))
                                }
                            } else {
                                continuation.resumeWithException(
                                    tokenTask.exception ?: Exception("Failed to retrieve Firebase ID token")
                                )
                            }
                        }
                    } else {
                        continuation.resumeWithException(Exception("Firebase user was null after sign-in"))
                    }
                } else {
                    continuation.resumeWithException(
                        task.exception ?: Exception("Firebase authentication failed")
                    )
                }
            }
    }

    /**
     * Registers a new user with Firebase using Email + Password and returns the UID.
     */
    suspend fun createUser(email: String, password: String): String = suspendCancellableCoroutine { continuation ->
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = task.result?.user
                    if (user != null) {
                        continuation.resume(user.uid)
                    } else {
                        continuation.resumeWithException(Exception("Firebase user was null after creation"))
                    }
                } else {
                    continuation.resumeWithException(
                        task.exception ?: Exception("Firebase user registration failed")
                    )
                }
            }
    }

    /**
     * Logs out the user from Firebase Auth.
     */
    fun signOut() {
        firebaseAuth.signOut()
    }
}
