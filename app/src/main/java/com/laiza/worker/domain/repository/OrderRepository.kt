package com.laiza.worker.domain.repository

import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.KaarigerOrder
import com.laiza.worker.domain.models.KaarigerOrderPayment
import com.laiza.worker.domain.models.OrderApprovalRecord
import com.laiza.worker.domain.models.OrderMaterial
import kotlinx.coroutines.flow.Flow

interface OrderRepository {
    fun getAllOrders(): Flow<List<KaarigerOrder>>
    fun getOrdersForKaariger(kaarigerId: String): Flow<List<KaarigerOrder>>
    fun getPendingApprovalOrders(): Flow<List<KaarigerOrder>>
    fun createOrder(order: KaarigerOrder): Flow<Resource<Unit>>
    fun submitDelivery(
        orderId: String,
        quantity: Int,
        color: String,
        productName: String,
        notes: String?
    ): Flow<Resource<Unit>>
    fun approveOrder(orderId: String, verifiedBy: String, verifiedByPhone: String): Flow<Resource<Unit>>
    fun rejectOrder(orderId: String, verifiedBy: String, reason: String): Flow<Resource<Unit>>
    fun submitMaterialUsage(orderId: String, materials: List<OrderMaterial>): Flow<Resource<Unit>>
    fun getApprovalHistoryForStaff(staffPhone: String): Flow<List<OrderApprovalRecord>>

    fun getPaymentsForOrder(orderId: String): Flow<List<KaarigerOrderPayment>>
    fun getPaymentsForKaariger(kaarigerId: String): Flow<List<KaarigerOrderPayment>>
    fun addPayment(payment: KaarigerOrderPayment): Flow<Resource<Unit>>
}
