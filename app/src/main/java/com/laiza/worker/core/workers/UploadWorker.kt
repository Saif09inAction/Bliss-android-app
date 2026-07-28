package com.laiza.worker.core.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class UploadWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_FILE_PATH = "key_file_path"
        const val KEY_UPLOAD_URL = "key_upload_url"
    }

    override suspend fun doWork(): Result {
        val filePath = inputData.getString(KEY_FILE_PATH)
        val uploadUrl = inputData.getString(KEY_UPLOAD_URL)

        if (filePath.isNullOrBlank() || uploadUrl.isNullOrBlank()) {
            Timber.e("Missing required upload parameters")
            return Result.failure()
        }

        Timber.d("Starting upload job for: $filePath to: $uploadUrl")

        return try {
            // Placeholder: Future image / file uploading logic using Retrofit multipart
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Upload job failed")
            Result.retry()
        }
    }
}
