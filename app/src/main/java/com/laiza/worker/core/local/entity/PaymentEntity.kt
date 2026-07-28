package com.laiza.worker.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.laiza.worker.domain.models.PaymentTransaction
import com.laiza.worker.domain.models.PaymentType

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey
    val id: String,
    val employeeId: String,
    val amount: Double,
    val type: String,
    val date: String,
    val time: String,
    val remarks: String?,
    val createdBy: String
) {
    fun toDomain(): PaymentTransaction {
        return PaymentTransaction(
            id = id,
            employeeId = employeeId,
            amount = amount,
            type = PaymentType.valueOf(type),
            date = date,
            time = time,
            remarks = remarks,
            createdBy = createdBy
        )
    }

    companion object {
        fun fromDomain(domain: PaymentTransaction): PaymentEntity {
            return PaymentEntity(
                id = domain.id,
                employeeId = domain.employeeId,
                amount = domain.amount,
                type = domain.type.name,
                date = domain.date,
                time = domain.time,
                remarks = domain.remarks,
                createdBy = domain.createdBy
            )
        }
    }
}
