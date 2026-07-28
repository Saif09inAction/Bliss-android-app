package com.laiza.worker.domain.models

import java.util.UUID

data class WorkOrder(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val productName: String,
    val productImage: String?,
    val requiredQuantity: Int,
    val completedQuantity: Int,
    val priority: WorkPriority,
    val status: WorkStatus,
    val deadline: String,
    val assignedDate: String,
    val assignedBy: String,
    val employeeId: String,
    val materials: List<AssignedMaterial>,
    val progressLogs: List<ProductionProgress> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class AssignedMaterial(
    val id: String = UUID.randomUUID().toString(),
    val workOrderId: String,
    val materialId: String,
    val name: String,
    val assignedQuantity: Double,
    val usedQuantity: Double,
    val remainingQuantity: Double,
    val wasteQuantity: Double,
    val unit: String
)

data class MaterialUsage(
    val materialId: String,
    val usedQuantity: Double,
    val remainingQuantity: Double,
    val wasteQuantity: Double
)

data class ProductionProgress(
    val id: String = UUID.randomUUID().toString(),
    val workOrderId: String,
    val completedPieces: Int,
    val progressPercentage: Int,
    val remarks: String?,
    val photoPath: String?,
    val timestamp: Long = System.currentTimeMillis()
)

enum class WorkPriority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}

enum class WorkStatus {
    PENDING,
    ACCEPTED,
    IN_PROGRESS,
    PAUSED,
    COMPLETED,
    CANCELLED,
    REJECTED
}
