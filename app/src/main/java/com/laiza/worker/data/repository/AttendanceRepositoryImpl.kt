package com.laiza.worker.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.core.utils.FirebaseStorageHelper
import com.laiza.worker.core.local.dao.AttendanceDao
import com.laiza.worker.core.local.entity.AttendanceEntity
import com.laiza.worker.core.local.entity.AttendanceSettingsEntity
import com.laiza.worker.domain.models.Attendance
import com.laiza.worker.domain.models.AttendanceStatus
import com.laiza.worker.domain.models.AttendanceSettings
import com.laiza.worker.domain.repository.AttendanceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AttendanceRepositoryImpl @Inject constructor(
    private val attendanceDao: AttendanceDao,
    private val firestore: FirebaseFirestore,
    private val storageHelper: FirebaseStorageHelper,
    @ApplicationContext private val context: Context
) : AttendanceRepository {

    override fun getAttendanceRecord(id: String): Flow<Attendance?> {
        return attendanceDao.getAttendanceById(id).map { it?.toDomain() }
    }

    override fun getEmployeeAttendanceHistory(employeeId: String): Flow<List<Attendance>> {
        // Trigger background sync
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = fetchEmployeeAttendanceFromFirestore(employeeId)
                for (att in list) {
                    val local = attendanceDao.getAttendanceById(att.id).first()
                    if (local != null) {
                        if (!local.signOutTime.isNullOrBlank() && att.signOutTime.isNullOrBlank()) {
                            continue
                        }
                    }
                    attendanceDao.insertAttendance(AttendanceEntity.fromDomain(att))
                }
            } catch (e: Exception) {
                // Ignore sync error
            }
        }
        return attendanceDao.getEmployeeAttendanceHistory(employeeId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getTodayAttendance(): Flow<List<Attendance>> {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = fetchTodayAttendanceFromFirestore(todayStr)
                for (att in list) {
                    val local = attendanceDao.getAttendanceById(att.id).first()
                    if (local != null) {
                        if (!local.signOutTime.isNullOrBlank() && att.signOutTime.isNullOrBlank()) {
                            continue
                        }
                    }
                    attendanceDao.insertAttendance(AttendanceEntity.fromDomain(att))
                }
            } catch (e: Exception) {
                // Ignore sync error
            }
        }
        return attendanceDao.getTodayAttendance(todayStr).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun saveAttendance(attendance: Attendance): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Save local record immediately for instant UI update
            attendanceDao.insertAttendance(AttendanceEntity.fromDomain(attendance))

            // Perform background upload and firestore sync asynchronously
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    var finalAttendance = attendance
                    val localPath = attendance.signInImageLocalPath
                    if (!localPath.isNullOrBlank() && !localPath.startsWith("http")) {
                        val downloadUrl = storageHelper.uploadImage(context, localPath, "attendance_selfies")
                        if (downloadUrl != null) {
                            finalAttendance = attendance.copy(signInImageLocalPath = downloadUrl)
                            // Update Room with storage URL
                            attendanceDao.insertAttendance(AttendanceEntity.fromDomain(finalAttendance))
                        }
                    }
                    saveAttendanceToFirestore(finalAttendance)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to save attendance"))
        }
    }

    override fun getSettings(): Flow<AttendanceSettings> {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = fetchSettingsFromFirestore()
                if (settings != null) {
                    attendanceDao.saveSettings(AttendanceSettingsEntity("settings_id", settings.dailySignInTime, settings.dailySignOutTime))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return attendanceDao.getSettings().map {
            it?.toDomain() ?: AttendanceSettings("09:00", "18:00")
        }
    }

    override fun saveSettings(settings: AttendanceSettings): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val data = hashMapOf(
                "dailySignInTime" to settings.dailySignInTime,
                "dailySignOutTime" to settings.dailySignOutTime
            )
            try {
                saveSettingsToFirestore(data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            attendanceDao.saveSettings(AttendanceSettingsEntity("settings_id", settings.dailySignInTime, settings.dailySignOutTime))
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to save settings"))
        }
    }

    private suspend fun fetchSettingsFromFirestore(): AttendanceSettings? = suspendCancellableCoroutine { continuation ->
        firestore.collection("settings").document("attendance").get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val inTime = doc.getString("dailySignInTime") ?: "09:00"
                    val outTime = doc.getString("dailySignOutTime") ?: "18:00"
                    continuation.resume(AttendanceSettings(inTime, outTime))
                } else {
                    continuation.resume(null)
                }
            }
            .addOnFailureListener { err ->
                continuation.resumeWithException(err)
            }
    }

    private suspend fun saveSettingsToFirestore(data: Map<String, Any>): Unit = suspendCancellableCoroutine { continuation ->
        firestore.collection("settings").document("attendance").set(data)
            .addOnSuccessListener { continuation.resume(Unit) }
            .addOnFailureListener { err -> continuation.resumeWithException(err) }
    }

    private suspend fun saveAttendanceToFirestore(attendance: Attendance): Unit = suspendCancellableCoroutine { continuation ->
        val data = hashMapOf(
            "id" to attendance.id,
            "employeeId" to attendance.employeeId,
            "date" to attendance.date,
            "signInTime" to (attendance.signInTime ?: ""),
            "signOutTime" to (attendance.signOutTime ?: ""),
            "signInGps" to (attendance.signInGps ?: ""),
            "signOutGps" to (attendance.signOutGps ?: ""),
            "signInAddress" to (attendance.signInAddress ?: ""),
            "signOutAddress" to (attendance.signOutAddress ?: ""),
            "signInImageLocalPath" to (attendance.signInImageLocalPath ?: ""),
            "signOutImageLocalPath" to (attendance.signOutImageLocalPath ?: ""),
            "status" to attendance.status.name,
            "lateMinutes" to attendance.lateMinutes,
            "workingHours" to attendance.workingHours
        )
        firestore.collection("attendance").document(attendance.id).set(data)
            .addOnSuccessListener { continuation.resume(Unit) }
            .addOnFailureListener { err -> continuation.resumeWithException(err) }
    }

    private suspend fun fetchEmployeeAttendanceFromFirestore(employeeId: String): List<Attendance> = suspendCancellableCoroutine { continuation ->
        firestore.collection("attendance").whereEqualTo("employeeId", employeeId).get()
            .addOnSuccessListener { querySnapshot ->
                val list = querySnapshot.map { doc ->
                    Attendance(
                        id = doc.getString("id") ?: doc.id,
                        employeeId = doc.getString("employeeId") ?: "",
                        date = doc.getString("date") ?: "",
                        signInTime = doc.getString("signInTime"),
                        signOutTime = doc.getString("signOutTime"),
                        signInGps = doc.getString("signInGps"),
                        signOutGps = doc.getString("signOutGps"),
                        signInAddress = doc.getString("signInAddress"),
                        signOutAddress = doc.getString("signOutAddress"),
                        signInImageLocalPath = doc.getString("signInImageLocalPath"),
                        signOutImageLocalPath = doc.getString("signOutImageLocalPath"),
                        status = AttendanceStatus.valueOf(doc.getString("status") ?: "PRESENT"),
                        lateMinutes = doc.getLong("lateMinutes")?.toInt() ?: 0,
                        workingHours = doc.getDouble("workingHours") ?: 0.0
                    )
                }
                continuation.resume(list)
            }
            .addOnFailureListener { err ->
                continuation.resumeWithException(err)
            }
    }

    private suspend fun fetchTodayAttendanceFromFirestore(date: String): List<Attendance> = suspendCancellableCoroutine { continuation ->
        firestore.collection("attendance").whereEqualTo("date", date).get()
            .addOnSuccessListener { querySnapshot ->
                val list = querySnapshot.map { doc ->
                    Attendance(
                        id = doc.getString("id") ?: doc.id,
                        employeeId = doc.getString("employeeId") ?: "",
                        date = doc.getString("date") ?: "",
                        signInTime = doc.getString("signInTime"),
                        signOutTime = doc.getString("signOutTime"),
                        signInGps = doc.getString("signInGps"),
                        signOutGps = doc.getString("signOutGps"),
                        signInAddress = doc.getString("signInAddress"),
                        signOutAddress = doc.getString("signOutAddress"),
                        signInImageLocalPath = doc.getString("signInImageLocalPath"),
                        signOutImageLocalPath = doc.getString("signOutImageLocalPath"),
                        status = AttendanceStatus.valueOf(doc.getString("status") ?: "PRESENT"),
                        lateMinutes = doc.getLong("lateMinutes")?.toInt() ?: 0,
                        workingHours = doc.getDouble("workingHours") ?: 0.0
                    )
                }
                continuation.resume(list)
            }
            .addOnFailureListener { err ->
                continuation.resumeWithException(err)
            }
    }
}
