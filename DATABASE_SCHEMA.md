# Database Schema Design - Laiza Room Database

This document details the Room Database Schema for **Laiza**. The entities are designed to be offline-first and easily mappable to Firebase Firestore documents in the future.

---

## 1. Entity Diagrams & Tables

### `employees`
Stores employee profiles, roles, and current salary configuration.

| Field | Type | Room Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | String | PRIMARY KEY | Unique ID (corresponds to Firebase UID or generated UUID) |
| `name` | String | NOT NULL | Employee name |
| `phone` | String | NOT NULL, UNIQUE | Mobile number used for authentication |
| `role` | String | NOT NULL | `ADMIN` or `EMPLOYEE` |
| `joiningDate` | String | NOT NULL | Date format `yyyy-MM-dd` |
| `monthlySalary` | Double | NOT NULL | Base monthly salary amount |
| `profilePhotoUrl` | String | NULLABLE | Local path or Cloud Storage URL of profile photo |
| `attendancePercentage` | Double | DEFAULT 0.0 | Calculated attendance metric |

---

### `attendance`
Stores individual attendance records for employee sign-in and sign-out events.

| Field | Type | Room Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | String | PRIMARY KEY | Unique record ID (UUID) |
| `employeeId` | String | FOREIGN KEY -> `employees(id)` | Associated employee |
| `date` | String | NOT NULL | Date format `yyyy-MM-dd` |
| `signInTime` | String | NULLABLE | Sign-in time format `HH:mm:ss` |
| `signOutTime` | String | NULLABLE | Sign-out time format `HH:mm:ss` |
| `signInGps` | String | NULLABLE | Latitude, Longitude (e.g., `19.0760,72.8777`) |
| `signOutGps` | String | NULLABLE | Latitude, Longitude |
| `signInAddress` | String | NULLABLE | Geocoded text address of sign-in |
| `signOutAddress` | String | NULLABLE | Geocoded text address of sign-out |
| `signInImageLocalPath` | String | NULLABLE | Local device photo path (purged after 10 days) |
| `signOutImageLocalPath`| String | NULLABLE | Local device photo path (purged after 10 days) |
| `status` | String | NOT NULL | `PRESENT`, `LATE`, `LEFT_EARLY`, `ON_TIME`, `ABSENT` |
| `lateMinutes` | Int | DEFAULT 0 | Calculation: `signInTime - dailySignInTime` |
| `workingHours` | Double | DEFAULT 0.0 | Calculated: `signOutTime - signInTime` |

---

### `attendance_settings`
Singleton table representing configurations managed by Admin.

| Field | Type | Room Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | String | PRIMARY KEY (Always `"singleton"`) | Enforces a single row |
| `dailySignInTime` | String | NOT NULL | Expected start time (e.g., `"09:00"`) |
| `dailySignOutTime` | String | NOT NULL | Expected end time (e.g., `"18:00"`) |

---

### `payments`
Transactions ledger tracking salary payments, advances, extra payments, and deductions.

| Field | Type | Room Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | String | PRIMARY KEY | Unique transaction ID (UUID) |
| `employeeId` | String | FOREIGN KEY -> `employees(id)` | Associated employee |
| `amount` | Double | NOT NULL | Monetary value of the transaction |
| `type` | String | NOT NULL | `SALARY_PAYMENT`, `ADVANCE`, `EXTRA_PAYMENT`, `DEDUCTION` |
| `date` | String | NOT NULL | Date of transaction `yyyy-MM-dd` |
| `time` | String | NOT NULL | Time of transaction `HH:mm:ss` |
| `remarks` | String | NULLABLE | Description or notes |
| `createdBy` | String | NOT NULL | Name of Admin who created it |

---

### `raw_materials`
Stores raw material stock levels and metadata.

| Field | Type | Room Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | String | PRIMARY KEY | Unique material ID (UUID) |
| `materialName` | String | NOT NULL, UNIQUE | Name (e.g., "Leather", "Zip") |
| `quantity` | Double | NOT NULL | Current stock quantity |
| `unit` | String | NOT NULL | Unit of measurement (e.g., "Meter", "Pieces", "Kg") |
| `minimumStock` | Double | NOT NULL | Minimum safety stock before alert |
| `supplier` | String | NOT NULL | Vendor or supplier name |
| `lastUpdatedBy` | String | NOT NULL | Name of user who modified last |
| `lastUpdatedTime` | Long | NOT NULL | Epoch timestamp in milliseconds |

---

### `finished_products`
Stores final goods produced.

| Field | Type | Room Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | String | PRIMARY KEY | Unique product ID (UUID) |
| `productName` | String | NOT NULL, UNIQUE | Product identifier (e.g., "Classic Tote Bag") |
| `quantity` | Int | NOT NULL | Stock quantity available |
| `lastUpdatedBy` | String | NOT NULL | Name of user who modified last |
| `lastUpdatedTime` | Long | NOT NULL | Epoch timestamp in milliseconds |

---

### `finished_product_raw_materials`
Cross-reference table linking raw materials used to compose a finished product.

| Field | Type | Room Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | String | PRIMARY KEY | Composite or UUID relationship key |
| `finishedProductId` | String | FOREIGN KEY -> `finished_products(id)` ON DELETE CASCADE | Associated finished product |
| `rawMaterialId` | String | FOREIGN KEY -> `raw_materials(id)` | Associated raw material consumed |
| `quantityUsed` | Double | NOT NULL | Quantity of raw material consumed per 1 unit of finished product |

---

### `activity_logs`
System-wide audit trail of operations.

| Field | Type | Room Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | String | PRIMARY KEY | Unique log ID (UUID) |
| `userName` | String | NOT NULL | Name of user performing action |
| `action` | String | NOT NULL | Action description |
| `module` | String | NOT NULL | `EMPLOYEE`, `SALARY`, `ATTENDANCE`, `STOCK`, `PAYMENT` |
| `date` | String | NOT NULL | Date format `yyyy-MM-dd` |
| `time` | String | NOT NULL | Time format `HH:mm:ss` |

---

### `notifications`
Alerts generated when actions occur.

| Field | Type | Room Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | String | PRIMARY KEY | Unique notification ID (UUID) |
| `employeeId` | String | NULLABLE (Null = Broadcast to all) | Target recipient |
| `title` | String | NOT NULL | Title header |
| `message` | String | NOT NULL | Text content |
| `date` | String | NOT NULL | Date format `yyyy-MM-dd` |
| `time` | String | NOT NULL | Time format `HH:mm:ss` |
| `isRead` | Boolean | DEFAULT 0 | Status of notification read status |

---

### `offline_sync_queue`
Mutation queue to track local changes for future Firebase synchronization.

| Field | Type | Room Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | String | PRIMARY KEY | Unique tracking ID (UUID) |
| `entityType` | String | NOT NULL | e.g. `"EMPLOYEE"`, `"ATTENDANCE"`, `"PAYMENT"`, `"RAW_MATERIAL"` |
| `entityId` | String | NOT NULL | Associated local database record ID |
| `operation` | String | NOT NULL | `CREATE`, `UPDATE`, `DELETE` |
| `status` | String | DEFAULT `'PENDING'` | `PENDING`, `SYNCED`, `FAILED` |
| `timestamp` | Long | NOT NULL | Epoch time of mutation |

---

## 2. Relationships & Integrity Rules

1. **Cascade Deletes**: Deleting a finished product cascades and deletes its raw material recipe lists (`finished_product_raw_materials`).
2. **Restricted Deletes**: Deleting a raw material should be blocked if it is referenced in an active finished product recipe to prevent database inconsistency.
3. **Transaction Safety**: Adding a Finished Product decrements `raw_materials.quantity` and increments `finished_products.quantity` simultaneously inside a Room `@Transaction` block to guarantee atomic consistency.
