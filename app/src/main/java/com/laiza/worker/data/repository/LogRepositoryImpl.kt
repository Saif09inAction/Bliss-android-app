package com.laiza.worker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.laiza.worker.core.local.dao.AuditLogDao
import com.laiza.worker.core.local.entity.ActivityLogEntity
import com.laiza.worker.domain.models.ActivityLog
import com.laiza.worker.domain.repository.LogRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import com.laiza.worker.core.utils.DateFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LogRepositoryImpl @Inject constructor(
    private val auditLogDao: AuditLogDao,
    private val firestore: FirebaseFirestore
) : LogRepository {

    override fun getLogs(): Flow<List<ActivityLog>> {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = fetchLogsFromFirestore()
                for (item in list) {
                    auditLogDao.insertLog(ActivityLogEntity.fromDomain(item))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return auditLogDao.getAllLogs().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun addLog(userName: String, action: String, module: String) {
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val timeStr = DateFormatter.nowTime12HourWithSeconds()
        val id = UUID.randomUUID().toString()
        val log = ActivityLog(
            id = id,
            userName = userName,
            action = action,
            module = module,
            date = dateStr,
            time = timeStr
        )
        
        try {
            saveLogToFirestore(log)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        auditLogDao.insertLog(ActivityLogEntity.fromDomain(log))
    }

    private suspend fun saveLogToFirestore(log: ActivityLog): Unit = suspendCancellableCoroutine { continuation ->
        val data = hashMapOf(
            "id" to log.id,
            "userName" to log.userName,
            "action" to log.action,
            "module" to log.module,
            "date" to log.date,
            "time" to log.time
        )
        firestore.collection("audit_logs").document(log.id).set(data)
            .addOnSuccessListener {
                continuation.resume(Unit)
            }
            .addOnFailureListener { err ->
                continuation.resumeWithException(err)
            }
    }

    private suspend fun fetchLogsFromFirestore(): List<ActivityLog> = suspendCancellableCoroutine { continuation ->
        firestore.collection("audit_logs").get()
            .addOnSuccessListener { querySnapshot ->
                val list = querySnapshot.map { doc ->
                    ActivityLog(
                        id = doc.getString("id") ?: doc.id,
                        userName = doc.getString("userName") ?: "",
                        action = doc.getString("action") ?: "",
                        module = doc.getString("module") ?: "",
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: ""
                    )
                }
                continuation.resume(list)
            }
            .addOnFailureListener { err ->
                continuation.resumeWithException(err)
            }
    }
}
