package com.laiza.worker.core.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.laiza.worker.domain.models.RawMaterial

@Entity(tableName = "raw_materials")
data class RawMaterialEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val quantity: Double,
    val unit: String,
    val minimumStock: Double,
    val supplier: String,
    val lastUpdatedBy: String,
    val lastUpdatedTime: Long,
    val imagePath: String? = null
) {
    fun toDomain(): RawMaterial {
        return RawMaterial(
            id = id,
            name = name,
            quantity = quantity,
            unit = unit,
            minimumStock = minimumStock,
            supplier = supplier,
            lastUpdatedBy = lastUpdatedBy,
            lastUpdatedTime = lastUpdatedTime,
            imagePath = imagePath
        )
    }

    companion object {
        fun fromDomain(domain: RawMaterial): RawMaterialEntity {
            return RawMaterialEntity(
                id = domain.id,
                name = domain.name,
                quantity = domain.quantity,
                unit = domain.unit,
                minimumStock = domain.minimumStock,
                supplier = domain.supplier,
                lastUpdatedBy = domain.lastUpdatedBy,
                lastUpdatedTime = domain.lastUpdatedTime,
                imagePath = domain.imagePath
            )
        }
    }
}
