package com.laiza.worker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.core.local.dao.EmployeeDao
import com.laiza.worker.core.local.dao.PaymentDao
import com.laiza.worker.core.local.entity.PaymentEntity
import com.laiza.worker.domain.models.PaymentTransaction
import com.laiza.worker.domain.models.PaymentType
import com.laiza.worker.domain.models.SalaryBalanceSheet
import com.laiza.worker.domain.repository.PaymentRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
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

import com.laiza.worker.core.local.dao.AttendanceDao
import com.laiza.worker.core.local.entity.AttendanceEntity
import com.laiza.worker.domain.models.Attendance
import java.util.Calendar

class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao,
    private val employeeDao: EmployeeDao,
    private val attendanceDao: AttendanceDao,
    private val firestore: FirebaseFirestore
) : PaymentRepository {

    override fun getPaymentsForEmployee(employeeId: String): Flow<List<PaymentTransaction>> {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = fetchPaymentsFromFirestore(employeeId)
                for (payment in list) {
                    paymentDao.insertPayment(PaymentEntity.fromDomain(payment))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return paymentDao.getPaymentsForEmployee(employeeId).map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getAllTransactions(): Flow<List<PaymentTransaction>> {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = fetchAllPaymentsFromFirestore()
                for (payment in list) {
                    paymentDao.insertPayment(PaymentEntity.fromDomain(payment))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return paymentDao.getAllTransactions().map { list ->
            list.map { it.toDomain() }
        }
    }

    private data class PayPeriodData(
        val start: String,
        val end: String,
        val daysInPeriod: Int
    )

    private fun resolvePayPeriod(joiningDateStr: String, todayStr: String): PayPeriodData {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val joinDate = try { sdf.parse(joiningDateStr) } catch (_: Exception) { null }
        val todayDate = try { sdf.parse(todayStr) } catch (_: Exception) { Date() }

        if (joinDate == null) {
            val cal = Calendar.getInstance().apply { time = todayDate; set(Calendar.DAY_OF_MONTH, 1) }
            val start = sdf.format(cal.time)
            val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
            cal.set(Calendar.DAY_OF_MONTH, maxDay)
            val end = sdf.format(cal.time)
            return PayPeriodData(start, end, maxDay)
        }

        val calJoin = Calendar.getInstance().apply { time = joinDate }
        val joinDay = calJoin.get(Calendar.DAY_OF_MONTH)

        val calToday = Calendar.getInstance().apply { time = todayDate }
        val todayYear = calToday.get(Calendar.YEAR)
        val todayMonth = calToday.get(Calendar.MONTH)
        val todayDay = calToday.get(Calendar.DAY_OF_MONTH)

        val calStart = Calendar.getInstance().apply {
            set(Calendar.YEAR, todayYear)
            set(Calendar.MONTH, todayMonth)
            val maxDaysThisMonth = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, minOf(joinDay, maxDaysThisMonth))
        }

        if (todayDay < joinDay && calToday.before(calStart)) {
            calStart.add(Calendar.MONTH, -1)
            val maxDaysPrevMonth = calStart.getActualMaximum(Calendar.DAY_OF_MONTH)
            calStart.set(Calendar.DAY_OF_MONTH, minOf(joinDay, maxDaysPrevMonth))
        }

        val calEnd = (calStart.clone() as Calendar).apply {
            add(Calendar.MONTH, 1)
            add(Calendar.DAY_OF_MONTH, -1)
        }

        val startStr = sdf.format(calStart.time)
        val endStr = sdf.format(calEnd.time)

        val diffMs = calEnd.timeInMillis - calStart.timeInMillis
        val daysInPeriod = (diffMs / (1000 * 60 * 60 * 24)).toInt() + 1

        return PayPeriodData(startStr, endStr, maxOf(1, daysInPeriod))
    }

    private suspend fun fetchAttendanceFromFirestore(employeeId: String): List<Attendance> = suspendCancellableCoroutine { continuation ->
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
                        status = com.laiza.worker.domain.models.parseAttendanceStatus(doc.getString("status") ?: "ABSENT"),
                        lateMinutes = (doc.getLong("lateMinutes") ?: 0L).toInt(),
                        workingHours = doc.getDouble("workingHours") ?: 0.0
                    )
                }
                continuation.resume(list)
            }
            .addOnFailureListener { err ->
                continuation.resumeWithException(err)
            }
    }

    override fun getSalaryBalanceSheet(employeeId: String): Flow<SalaryBalanceSheet> {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val attList = fetchAttendanceFromFirestore(employeeId)
                for (att in attList) {
                    attendanceDao.insertAttendance(AttendanceEntity.fromDomain(att))
                }
                val payList = fetchPaymentsFromFirestore(employeeId)
                for (pay in payList) {
                    paymentDao.insertPayment(PaymentEntity.fromDomain(pay))
                }
            } catch (_: Exception) {}
        }

        val employeeFlow = employeeDao.getEmployeeById(employeeId)
        val paymentsFlow = paymentDao.getPaymentsForEmployee(employeeId)
        val attendanceFlow = attendanceDao.getEmployeeAttendanceHistory(employeeId)

        return combine(employeeFlow, paymentsFlow, attendanceFlow) { employee, payments, attendanceEntities ->
            val baseMonthlySalary = employee?.monthlySalary ?: 0.0
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val joiningDateStr = employee?.joiningDate?.trim().takeIf { !it.isNullOrBlank() } ?: todayStr

            val payPeriod = resolvePayPeriod(joiningDateStr, todayStr)
            val daysInPeriod = maxOf(payPeriod.daysInPeriod, 1)
            val perDayRate = if (baseMonthlySalary > 0.0) baseMonthlySalary / daysInPeriod else 0.0

            val attendanceList = attendanceEntities.map { it.toDomain() }
            val periodAttendance = attendanceList.filter { it.date >= payPeriod.start && it.date <= todayStr }

            var earnedSalary = 0.0
            for (att in periodAttendance) {
                val hasPunch = !att.signInTime.isNullOrBlank()
                if (!hasPunch) continue

                val dayFactor = if (att.status.name == "HALF_DAY") 0.5 else 1.0
                earnedSalary += perDayRate * dayFactor
            }

            var salaryReceived = 0.0
            var advanceTaken = 0.0
            var extraPayments = 0.0
            var deductions = 0.0

            val periodPayments = payments.filter { it.date >= payPeriod.start }
            for (payment in periodPayments) {
                when (payment.type) {
                    PaymentType.SALARY_PAYMENT.name -> salaryReceived += payment.amount
                    PaymentType.ADVANCE.name -> advanceTaken += payment.amount
                    PaymentType.EXTRA_PAYMENT.name -> extraPayments += payment.amount
                    PaymentType.DEDUCTION.name -> deductions += payment.amount
                }
            }

            val salaryRemaining = earnedSalary + extraPayments - salaryReceived - advanceTaken - deductions

            SalaryBalanceSheet(
                employeeId = employeeId,
                monthlySalary = baseMonthlySalary,
                salaryReceived = salaryReceived + advanceTaken,
                salaryRemaining = salaryRemaining,
                advanceTaken = advanceTaken,
                extraPayments = extraPayments,
                pendingSalary = salaryRemaining
            )
        }
    }

    override fun addPayment(
        employeeId: String,
        amount: Double,
        type: PaymentType,
        remarks: String?,
        adminName: String
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val timeStr = DateFormatter.nowTime12HourWithSeconds()
            val payment = PaymentTransaction(
                id = UUID.randomUUID().toString(),
                employeeId = employeeId,
                amount = amount,
                type = type,
                date = dateStr,
                time = timeStr,
                remarks = remarks,
                createdBy = adminName
            )

            try {
                savePaymentToFirestore(payment)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            paymentDao.insertPayment(PaymentEntity.fromDomain(payment))
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to record payment"))
        }
    }

    private suspend fun fetchPaymentsFromFirestore(employeeId: String): List<PaymentTransaction> = suspendCancellableCoroutine { continuation ->
        firestore.collection("payments").whereEqualTo("employeeId", employeeId).get()
            .addOnSuccessListener { querySnapshot ->
                val list = querySnapshot.map { doc ->
                    PaymentTransaction(
                        id = doc.getString("id") ?: doc.id,
                        employeeId = doc.getString("employeeId") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        type = PaymentType.valueOf(doc.getString("type") ?: "SALARY_PAYMENT"),
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        remarks = doc.getString("remarks"),
                        createdBy = doc.getString("createdBy") ?: ""
                    )
                }
                continuation.resume(list)
            }
            .addOnFailureListener { err ->
                continuation.resumeWithException(err)
            }
    }

    private suspend fun fetchAllPaymentsFromFirestore(): List<PaymentTransaction> = suspendCancellableCoroutine { continuation ->
        firestore.collection("payments").get()
            .addOnSuccessListener { querySnapshot ->
                val list = querySnapshot.map { doc ->
                    PaymentTransaction(
                        id = doc.getString("id") ?: doc.id,
                        employeeId = doc.getString("employeeId") ?: "",
                        amount = doc.getDouble("amount") ?: 0.0,
                        type = PaymentType.valueOf(doc.getString("type") ?: "SALARY_PAYMENT"),
                        date = doc.getString("date") ?: "",
                        time = doc.getString("time") ?: "",
                        remarks = doc.getString("remarks"),
                        createdBy = doc.getString("createdBy") ?: ""
                    )
                }
                continuation.resume(list)
            }
            .addOnFailureListener { err ->
                continuation.resumeWithException(err)
            }
    }

    private suspend fun savePaymentToFirestore(payment: PaymentTransaction): Unit = suspendCancellableCoroutine { continuation ->
        val data = hashMapOf(
            "id" to payment.id,
            "employeeId" to payment.employeeId,
            "amount" to payment.amount,
            "type" to payment.type.name,
            "date" to payment.date,
            "time" to payment.time,
            "remarks" to (payment.remarks ?: ""),
            "createdBy" to payment.createdBy
        )
        firestore.collection("payments").document(payment.id).set(data)
            .addOnSuccessListener { continuation.resume(Unit) }
            .addOnFailureListener { err -> continuation.resumeWithException(err) }
    }
}
