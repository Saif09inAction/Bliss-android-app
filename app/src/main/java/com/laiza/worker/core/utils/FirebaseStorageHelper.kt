package com.laiza.worker.core.utils

import android.content.Context
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class FirebaseStorageHelper @Inject constructor(
    private val storage: FirebaseStorage
) {
    /**
     * Uploads an image from a local Uri or file path to Firebase Storage and returns the public download URL.
     * Fallbacks to the input string if upload fails or is already a web URL.
     * Compresses the image to save storage space.
     */
    suspend fun uploadImage(context: Context, uriString: String?, folderName: String): String? {
        if (uriString.isNullOrBlank()) return null
        
        // If it already looks like a web URL, don't re-upload
        if (uriString.startsWith("http://") || uriString.startsWith("https://")) {
            return uriString
        }

        return suspendCancellableCoroutine { continuation ->
            try {
                val uri = Uri.parse(uriString)
                val fileName = "${UUID.randomUUID()}.jpg"
                val ref = storage.reference.child("$folderName/$fileName")

                val compressedFile = compressImage(context, uri)
                if (compressedFile == null) {
                    // Fallback to uploading original directly if compression fails
                    val uploadTask = if (uri.scheme == "content" || uri.scheme == "file") {
                        ref.putFile(uri)
                    } else {
                        val file = File(uriString)
                        if (file.exists()) {
                            ref.putFile(Uri.fromFile(file))
                        } else {
                            continuation.resume(uriString)
                            return@suspendCancellableCoroutine
                        }
                    }

                    uploadTask.continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { throw it }
                        }
                        ref.downloadUrl
                    }.addOnSuccessListener { downloadUri ->
                        continuation.resume(downloadUri.toString())
                    }.addOnFailureListener { exception ->
                        continuation.resumeWithException(exception)
                    }
                } else {
                    val uploadTask = ref.putFile(Uri.fromFile(compressedFile))
                    uploadTask.continueWithTask { task ->
                        if (!task.isSuccessful) {
                            task.exception?.let { throw it }
                        }
                        ref.downloadUrl
                    }.addOnSuccessListener { downloadUri ->
                        compressedFile.delete()
                        continuation.resume(downloadUri.toString())
                    }.addOnFailureListener { exception ->
                        compressedFile.delete()
                        continuation.resumeWithException(exception)
                    }
                }
            } catch (e: Exception) {
                continuation.resumeWithException(e)
            }
        }
    }

    private fun compressImage(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap == null) return null

            // Create temporary file for compressed image
            val tempFile = File(context.cacheDir, "compressed_${UUID.randomUUID()}.jpg")
            val outputStream = java.io.FileOutputStream(tempFile)
            
            // Compress with 70% JPEG quality
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            outputStream.flush()
            outputStream.close()
            
            // Recycle bitmap to free up memory
            bitmap.recycle()
            
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
