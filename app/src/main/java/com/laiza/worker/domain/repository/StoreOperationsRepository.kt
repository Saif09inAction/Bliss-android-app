package com.laiza.worker.domain.repository

import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.DeliveryPartner
import com.laiza.worker.domain.models.PickupRecord
import com.laiza.worker.domain.models.ReturnRecord
import kotlinx.coroutines.flow.Flow

interface StoreOperationsRepository {
    fun getAllPickups(): Flow<List<PickupRecord>>
    fun getAllReturns(): Flow<List<ReturnRecord>>
    fun getDeliveryPartners(): Flow<List<DeliveryPartner>>
    fun addDeliveryPartner(name: String): Flow<Resource<DeliveryPartner>>
    fun recordPickup(record: PickupRecord): Flow<Resource<Unit>>
    fun recordReturn(record: ReturnRecord): Flow<Resource<Unit>>
}
