package com.laiza.worker.core.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.laiza.worker.core.local.entity.SaleRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Query("SELECT * FROM sale_records ORDER BY date DESC, time DESC")
    fun getAllSales(): Flow<List<SaleRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleRecordEntity)

    @Query("DELETE FROM sale_records WHERE id = :id")
    suspend fun deleteSaleById(id: String)
}
