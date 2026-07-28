# API Contracts & Repository Interfaces - Laiza

This document defines the clean architecture boundaries using Kotlin interfaces and data models. It acts as the API contract for data access.

---

## 1. Authentication Repository

Swapping between phone-number-to-email mapping (Room + Firebase Auth) and real SMS OTP credentials will only require swapping this implementation.

```kotlin
interface AuthRepository {
    /**
     * Authenticates a user using phone number and password.
     * Behind the scenes, the phone number is formatted to an email: [phone]@laiza.com
     */
    fun login(phone: String, password: String): Flow<Resource<UserSession>>

    /**
     * Signs out the current user session and clears local preferences.
     */
    fun logout(): Flow<Resource<Unit>>

    /**
     * Stream of the current user session, updating in real-time.
     */
    fun getCurrentSession(): Flow<UserSession?>
}

data class UserSession(
    val uid: String,
    val name: String,
    val phone: String,
    val role: Role,
    val token: String? = null
)

enum class Role {
    ADMIN, EMPLOYEE
}
```

---

## 2. Employee Management Repository

Manages employee registrations. Used exclusively by the Admin.

```kotlin
interface EmployeeRepository {
    fun getEmployee(id: String): Flow<Employee?>
    fun getAllEmployees(): Flow<List<Employee>>
    fun addEmployee(employee: Employee): Flow<Resource<Unit>>
    fun updateEmployee(employee: Employee): Flow<Resource<Unit>>
    fun deleteEmployee(id: String): Flow<Resource<Unit>>
    fun searchEmployees(query: String): Flow<List<Employee>>
}

data class Employee(
    val id: String,
    val name: String,
    val phone: String,
    val joiningDate: String,
    val monthlySalary: Double,
    val profilePhotoUrl: String?,
    val attendancePercentage: Double = 0.0
)
```

---

## 3. Attendance Repository

Handles clock-in and clock-out calculations. Evaluates late-in and early-out flags dynamically based on the current settings sheet.

```kotlin
interface AttendanceRepository {
    fun getAttendanceRecord(id: String): Flow<Attendance?>
    fun getEmployeeAttendanceHistory(employeeId: String): Flow<List<Attendance>>
    fun getTodayAttendance(): Flow<List<Attendance>>
    fun saveAttendance(attendance: Attendance): Flow<Resource<Unit>>
    
    // Settings configuration (Singleton access)
    fun getSettings(): Flow<AttendanceSettings>
    fun saveSettings(settings: AttendanceSettings): Flow<Resource<Unit>>
}

data class Attendance(
    val id: String,
    val employeeId: String,
    val date: String,
    val signInTime: String?,
    val signOutTime: String?,
    val signInGps: String?,
    val signOutGps: String?,
    val signInAddress: String?,
    val signOutAddress: String?,
    val signInImageLocalPath: String?,
    val signOutImageLocalPath: String?,
    val status: AttendanceStatus,
    val lateMinutes: Int = 0,
    val workingHours: Double = 0.0
)

data class AttendanceSettings(
    val dailySignInTime: String, // format "HH:mm" (e.g. "09:00")
    val dailySignOutTime: String  // format "HH:mm" (e.g. "18:00")
)

enum class AttendanceStatus {
    PRESENT, LATE, LEFT_EARLY, ON_TIME, ABSENT
}
```

---

## 4. Payment Management Repository

Handles payments, advances, extra payments, and salary ledger logs.

```kotlin
interface PaymentRepository {
    fun getPaymentsForEmployee(employeeId: String): Flow<List<PaymentTransaction>>
    fun getAllTransactions(): Flow<List<PaymentTransaction>>
    
    // Core balance sheets calculated from transactions
    fun getSalaryBalanceSheet(employeeId: String): Flow<SalaryBalanceSheet>
    
    fun addPayment(
        employeeId: String,
        amount: Double,
        type: PaymentType,
        remarks: String?,
        adminName: String
    ): Flow<Resource<Unit>>
}

data class PaymentTransaction(
    val id: String,
    val employeeId: String,
    val amount: Double,
    val type: PaymentType,
    val date: String,
    val time: String,
    val remarks: String?,
    val createdBy: String
)

data class SalaryBalanceSheet(
    val employeeId: String,
    val monthlySalary: Double,
    val salaryReceived: Double,
    val salaryRemaining: Double,
    val advanceTaken: Double,
    val extraPayments: Double,
    val pendingSalary: Double
)

enum class PaymentType {
    SALARY_PAYMENT, ADVANCE, EXTRA_PAYMENT, DEDUCTION
}
```

---

## 5. Inventory Repository (Raw Materials & Finished Products)

Executes transactions adjusting stock balances.

```kotlin
interface InventoryRepository {
    // Raw Materials
    fun getAllRawMaterials(): Flow<List<RawMaterial>>
    fun addRawMaterial(material: RawMaterial): Flow<Resource<Unit>>
    fun updateRawMaterial(material: RawMaterial): Flow<Resource<Unit>>
    fun deleteRawMaterial(id: String): Flow<Resource<Unit>>
    
    // Finished Products
    fun getAllFinishedProducts(): Flow<List<FinishedProduct>>
    
    /**
     * Saves a finished product and decreases raw material stocks inside a database transaction.
     */
    fun saveFinishedProduct(
        product: FinishedProduct,
        rawMaterialsUsed: List<RawMaterialConsumption>
    ): Flow<Resource<Unit>>
    
    fun deleteFinishedProduct(id: String): Flow<Resource<Unit>>
}

data class RawMaterial(
    val id: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val minimumStock: Double,
    val supplier: String,
    val lastUpdatedBy: String,
    val lastUpdatedTime: Long
)

data class FinishedProduct(
    val id: String,
    val name: String,
    val quantity: Int,
    val lastUpdatedBy: String,
    val lastUpdatedTime: Long
)

data class RawMaterialConsumption(
    val rawMaterialId: String,
    val rawMaterialName: String,
    val quantityUsed: Double
)
```

---

## 6. Audit & Log Repository

Internal audit logs tracking changes in the system.

```kotlin
interface LogRepository {
    fun getLogs(): Flow<List<ActivityLog>>
    fun addLog(userName: String, action: String, module: String): Flow<Unit>
}

data class ActivityLog(
    val id: String,
    val userName: String,
    val action: String,
    val module: String,
    val date: String,
    val time: String
)
```

---

## 7. Notification Repository

Exposes user-specific or global broadcast alert feeds.

```kotlin
interface NotificationRepository {
    fun getNotifications(employeeId: String?): Flow<List<NotificationAlert>>
    fun addNotification(notification: NotificationAlert): Flow<Unit>
    fun markAsRead(id: String): Flow<Unit>
}

data class NotificationAlert(
    val id: String,
    val employeeId: String?,
    val title: String,
    val message: String,
    val date: String,
    val time: String,
    val isRead: Boolean = false
)
```
