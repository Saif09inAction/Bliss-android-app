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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class PaymentRepositoryImpl @Inject constructor(
    private val paymentDao: PaymentDao,
    private val employeeDao: EmployeeDao,
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

    override fun getSalaryBalanceSheet(employeeId: String): Flow<SalaryBalanceSheet> {
        val employeeFlow = employeeDao.getEmployeeById(employeeId)
        val paymentsFlow = paymentDao.getPaymentsForEmployee(employeeId)

        return employeeFlow.combine(paymentsFlow) { employee, payments ->
            val baseMonthlySalary = employee?.monthlySalary ?: 0.0
            
            // Calculate completed months since joining date
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val joinDate = try { formatter.parse(employee?.joiningDate ?: "") } catch(e: Exception) { null }
            val monthsPassed = if (joinDate != null) {
                val calJoin = java.util.Calendar.getInstance().apply { time = joinDate }
                val calToday = java.util.Calendar.getInstance()
                var diffMonths = (calToday.get(java.util.Calendar.YEAR) - calJoin.get(java.util.Calendar.YEAR)) * 12 + (calToday.get(java.util.Calendar.MONTH) - calJoin.get(java.util.Calendar.MONTH))
                if (calToday.get(java.util.Calendar.DAY_OF_MONTH) < calJoin.get(java.util.Calendar.DAY_OF_MONTH)) {
                    diffMonths--
                }
                if (diffMonths < 0) 0 else diffMonths
            } else {
                0
            }

            val earnedSalary = monthsPassed * baseMonthlySalary
            
            var salaryReceived = 0.0
            var advanceTaken = 0.0
            var extraPayments = 0.0
            var deductions = 0.0

            for (payment in payments) {
                when (payment.type) {
                    PaymentType.SALARY_PAYMENT.name -> salaryReceived += payment.amount
                    PaymentType.ADVANCE.name -> advanceTaken += payment.amount
                    PaymentType.EXTRA_PAYMENT.name -> extraPayments += payment.amount
                    PaymentType.DEDUCTION.name -> deductions += payment.amount
                }
            }

            val salaryRemaining = earnedSalary + extraPayments - salaryReceived - deductions
            val pendingSalary = salaryRemaining - advanceTaken

            SalaryBalanceSheet(
                employeeId = employeeId,
                monthlySalary = baseMonthlySalary,
                salaryReceived = salaryReceived,
                salaryRemaining = salaryRemaining,
                advanceTaken = advanceTaken,
                extraPayments = extraPayments,
                pendingSalary = pendingSalary
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
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
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
