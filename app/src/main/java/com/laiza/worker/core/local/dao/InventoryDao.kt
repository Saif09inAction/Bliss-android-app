package com.laiza.worker.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.laiza.worker.core.local.entity.FinishedProductComponentEntity
import com.laiza.worker.core.local.entity.FinishedProductEntity
import com.laiza.worker.core.local.entity.RawMaterialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {

    // Raw Materials
    @Query("SELECT * FROM raw_materials ORDER BY name ASC")
    fun getAllRawMaterials(): Flow<List<RawMaterialEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRawMaterial(material: RawMaterialEntity)

    @Update
    suspend fun updateRawMaterial(material: RawMaterialEntity)

    @Query("DELETE FROM raw_materials WHERE id = :id")
    suspend fun deleteRawMaterialById(id: String)

    @Query("SELECT * FROM raw_materials WHERE id = :id")
    suspend fun getRawMaterialById(id: String): RawMaterialEntity?

    @Query("UPDATE raw_materials SET quantity = quantity - :deductQty WHERE id = :id")
    suspend fun deductRawMaterialQuantity(id: String, deductQty: Double)

    // Finished Products
    @Query("SELECT * FROM finished_products ORDER BY name ASC")
    fun getAllFinishedProducts(): Flow<List<FinishedProductEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinishedProduct(product: FinishedProductEntity)

    @Query("SELECT * FROM finished_products WHERE id = :id")
    suspend fun getFinishedProductById(id: String): FinishedProductEntity?

    @Query("DELETE FROM finished_products WHERE id = :id")
    suspend fun deleteFinishedProductById(id: String)

    @Query("UPDATE finished_products SET quantity = quantity - :deductQty WHERE id = :id")
    suspend fun deductFinishedProductQuantity(id: String, deductQty: Int)

    @Query("UPDATE finished_products SET quantity = quantity + :delta WHERE id = :id")
    suspend fun adjustFinishedProductQuantity(id: String, delta: Int)

    // Recipe connections
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFinishedProductComponent(component: FinishedProductComponentEntity)

    @Query("SELECT * FROM finished_product_raw_materials WHERE finishedProductId = :productId")
    fun getComponentsForProduct(productId: String): Flow<List<FinishedProductComponentEntity>>

    @Query("SELECT * FROM finished_product_raw_materials WHERE finishedProductId = :productId")
    suspend fun getComponentsForProductOnce(productId: String): List<FinishedProductComponentEntity>

    @Transaction
    suspend fun assembleFinishedProduct(
        product: FinishedProductEntity,
        components: List<FinishedProductComponentEntity>
    ) {
        // 1. Insert or update finished product
        val existingProduct = getFinishedProductById(product.id)
        if (existingProduct != null) {
            val updatedProduct = product.copy(quantity = existingProduct.quantity + product.quantity)
            insertFinishedProduct(updatedProduct)
        } else {
            insertFinishedProduct(product)
        }

        // 2. Loop through recipe items, deduct raw material stock, and record component recipe
        for (component in components) {
            insertFinishedProductComponent(component)
            deductRawMaterialQuantity(component.rawMaterialId, component.quantityUsed * product.quantity)
        }
    }
}
