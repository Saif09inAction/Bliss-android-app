package com.laiza.worker.core.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

sealed interface FaceValidationResult {
    data class Success(val imagePath: String) : FaceValidationResult
    data class Failure(val message: String) : FaceValidationResult
}

@Singleton
class FaceDetectorHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // Bundle the face detection model locally for offline operation
    private val detectorOptions = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .setMinFaceSize(0.15f)
        .build()

    private val detector = FaceDetection.getClient(detectorOptions)

    /**
     * Runs face detection and checks multiple quality metrics (rotation, open eyes, boundary margins, size, and lighting).
     */
    suspend fun detectAndValidateFace(imagePath: String): FaceValidationResult = withContext(Dispatchers.IO) {
        val file = File(imagePath)
        if (!file.exists()) {
            return@withContext FaceValidationResult.Failure("Selfie image file is missing.")
        }

        try {
            // Load dimensions of raw captured image
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(imagePath, options)
            val imgWidth = options.outWidth
            val imgHeight = options.outHeight

            if (imgWidth <= 0 || imgHeight <= 0) {
                return@withContext FaceValidationResult.Failure("Corrupted image file.")
            }

            // Perform Basic Lighting Brightness Check
            val brightnessOptions = BitmapFactory.Options().apply { inSampleSize = 8 }
            val downsampledBitmap = BitmapFactory.decodeFile(imagePath, brightnessOptions)
            if (downsampledBitmap != null) {
                var totalLuminance = 0L
                var pixelCount = 0
                for (x in 0 until downsampledBitmap.width) {
                    for (y in 0 until downsampledBitmap.height) {
                        val pixel = downsampledBitmap.getPixel(x, y)
                        val r = (pixel shr 16) and 0xff
                        val g = (pixel shr 8) and 0xff
                        val b = pixel and 0xff
                        // Standard luminance formula (Y = 0.299R + 0.587G + 0.114B)
                        val luminance = (0.299 * r + 0.587 * g + 0.114 * b).toLong()
                        totalLuminance += luminance
                        pixelCount++
                    }
                }
                val averageLuminance = if (pixelCount > 0) totalLuminance / pixelCount else 255
                // Dark lighting limit threshold
                if (averageLuminance < 35) {
                    return@withContext FaceValidationResult.Failure("Image is too dark. Move to a well-lit area.")
                }
            }

            // Run ML Kit Face Detection
            val image = InputImage.fromFilePath(context, Uri.fromFile(file))
            val faces = suspendCancellableCoroutine<List<Face>> { continuation ->
                detector.process(image)
                    .addOnSuccessListener { detectedFaces ->
                        if (continuation.isActive) {
                            continuation.resume(detectedFaces)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(emptyList())
                        }
                    }
            }

            // 1. Validate Face Count
            if (faces.isEmpty()) {
                return@withContext FaceValidationResult.Failure("No face detected. Please retake your selfie.")
            }
            if (faces.size > 1) {
                return@withContext FaceValidationResult.Failure("Multiple faces detected. Attendance cannot be marked.")
            }

            val face = faces[0]
            val bounds = face.boundingBox

            // 2. Validate Rotation Angle (User should look straight into camera)
            val yaw = face.headEulerAngleY   // horizontal rotation
            val tilt = face.headEulerAngleZ  // head tilt
            if (yaw > 22f || yaw < -22f || tilt > 18f || tilt < -18f) {
                return@withContext FaceValidationResult.Failure("Please face the camera directly.")
            }

            // 3. Validate Minimum Face Size (Ensure they are not too far)
            val faceWidthPercent = bounds.width().toFloat() / imgWidth
            if (faceWidthPercent < 0.22f) {
                return@withContext FaceValidationResult.Failure("Move closer to the camera.")
            }

            // 4. Validate Eyes Visibility (Ensure eyes are open and not closed/covered)
            val leftEyeProb = face.leftEyeOpenProbability
            val rightEyeProb = face.rightEyeOpenProbability
            if ((leftEyeProb != null && leftEyeProb < 0.4f) || (rightEyeProb != null && rightEyeProb < 0.4f)) {
                return@withContext FaceValidationResult.Failure("Eyes must be visible and open.")
            }

            // 5. Validate Boundary Overlap (Ensure face is not partially cropped out of the frame)
            val edgeBuffer = 12
            if (bounds.left < edgeBuffer || bounds.top < edgeBuffer || 
                bounds.right > imgWidth - edgeBuffer || bounds.bottom > imgHeight - edgeBuffer) {
                return@withContext FaceValidationResult.Failure("Your face must be completely visible and centered.")
            }

            // All checks passed!
            FaceValidationResult.Success(imagePath)
        } catch (e: Exception) {
            FaceValidationResult.Failure("Face detection processing failed: ${e.localizedMessage ?: "Unknown Error"}")
        }
    }
}
