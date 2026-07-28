package com.laiza.worker.core.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "finished_product_raw_materials",
    foreignKeys = [
        ForeignKey(
            entity = FinishedProductEntity::class,
            parentColumns = ["id"],
            childColumns = ["finishedProductId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = RawMaterialEntity::class,
            parentColumns = ["id"],
            childColumns = ["rawMaterialId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [
        Index(value = ["finishedProductId"]),
        Index(value = ["rawMaterialId"])
    ]
)
data class FinishedProductComponentEntity(
    @PrimaryKey
    val id: String,
    val finishedProductId: String,
    val rawMaterialId: String,
    val quantityUsed: Double
)
