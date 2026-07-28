package com.laiza.worker.core.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object CsvExportHelper {

    fun shareCsv(context: Context, fileName: String, csvContent: String) {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, fileName)
        file.writeText(csvContent)
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, fileName)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Export to Excel"))
    }

    fun escape(value: String?): String {
        val v = value ?: ""
        return if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
            "\"${v.replace("\"", "\"\"")}\""
        } else v
    }
}
