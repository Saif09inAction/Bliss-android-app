package com.laiza.worker.domain.repository

import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.FinishedProduct
import com.laiza.worker.domain.models.RawMaterial
import com.laiza.worker.domain.models.RawMaterialConsumption
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun getAllRawMaterials(): Flow<List<RawMaterial>>
    fun addRawMaterial(material: RawMaterial): Flow<Resource<Unit>>
    fun updateRawMaterial(material: RawMaterial): Flow<Resource<Unit>>
    fun deleteRawMaterial(id: String): Flow<Resource<Unit>>

    fun getAllFinishedProducts(): Flow<List<FinishedProduct>>
    fun saveFinishedProduct(
        product: FinishedProduct,
        rawMaterialsUsed: List<RawMaterialConsumption>
    ): Flow<Resource<Unit>>
    /** Manual stock entry — merges into existing name+color or creates new. */
    fun addManualFinishedProduct(
        name: String,
        color: String,
        quantity: Int,
        unitPrice: Double,
        updatedBy: String
    ): Flow<Resource<Unit>>
    fun deleteFinishedProduct(id: String): Flow<Resource<Unit>>
    fun adjustFinishedProductQuantity(productId: String, delta: Int): Flow<Resource<Unit>>
    fun refreshFinishedProducts(): Flow<Resource<Unit>>
}
