package com.laiza.worker.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.laiza.worker.domain.models.FinishedProduct

@Entity(tableName = "finished_products")
data class FinishedProductEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val quantity: Int,
    val lastUpdatedBy: String,
    val lastUpdatedTime: Long,
    val imagePath: String? = null,
    val unitPrice: Double = 0.0,
    val color: String = "",
    val orderId: String? = null
) {
    fun toDomain(): FinishedProduct {
        return FinishedProduct(
            id = id,
            name = name,
            quantity = quantity,
            lastUpdatedBy = lastUpdatedBy,
            lastUpdatedTime = lastUpdatedTime,
            imagePath = imagePath,
            unitPrice = unitPrice,
            color = color,
            orderId = orderId
        )
    }

    companion object {
        fun fromDomain(domain: FinishedProduct): FinishedProductEntity {
            return FinishedProductEntity(
                id = domain.id,
                name = domain.name,
                quantity = domain.quantity,
                lastUpdatedBy = domain.lastUpdatedBy,
                lastUpdatedTime = domain.lastUpdatedTime,
                imagePath = domain.imagePath,
                unitPrice = domain.unitPrice,
                color = domain.color,
                orderId = domain.orderId
            )
        }
    }
}
