package com.laiza.worker.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.laiza.worker.domain.models.SaleRecord

@Entity(tableName = "sale_records")
data class SaleRecordEntity(
    @PrimaryKey val id: String,
    val finishedProductId: String,
    val finishedProductName: String,
    val quantity: Int,
    val clientName: String,
    val clientPhone: String,
    val courierName: String,
    val trackingNumber: String,
    val date: String,
    val time: String,
    val soldBy: String,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0
) {
    fun toDomain() = SaleRecord(
        id = id,
        finishedProductId = finishedProductId,
        finishedProductName = finishedProductName,
        quantity = quantity,
        clientName = clientName,
        clientPhone = clientPhone,
        courierName = courierName,
        trackingNumber = trackingNumber,
        date = date,
        time = time,
        soldBy = soldBy,
        unitPrice = unitPrice,
        totalPrice = totalPrice
    )

    companion object {
        fun fromDomain(domain: SaleRecord) = SaleRecordEntity(
            id = domain.id,
            finishedProductId = domain.finishedProductId,
            finishedProductName = domain.finishedProductName,
            quantity = domain.quantity,
            clientName = domain.clientName,
            clientPhone = domain.clientPhone,
            courierName = domain.courierName,
            trackingNumber = domain.trackingNumber,
            date = domain.date,
            time = domain.time,
            soldBy = domain.soldBy,
            unitPrice = domain.unitPrice,
            totalPrice = domain.totalPrice
        )
    }
}
