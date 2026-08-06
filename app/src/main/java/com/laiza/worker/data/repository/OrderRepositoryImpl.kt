package com.laiza.worker.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.laiza.worker.core.local.dao.InventoryDao
import com.laiza.worker.core.local.entity.FinishedProductEntity
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.*
import com.laiza.worker.domain.repository.OrderRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class OrderRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val inventoryDao: InventoryDao
) : OrderRepository {

    override fun getAllOrders(): Flow<List<KaarigerOrder>> = ordersFlow(
        firestore.collection("kaariger_orders").orderBy("createdAt", Query.Direction.DESCENDING)
    )

    override fun getOrdersForKaariger(kaarigerId: String): Flow<List<KaarigerOrder>> {
        val normalizedId = normalizePhone(kaarigerId)
        return callbackFlow {
            val listener = firestore.collection("kaariger_orders")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Kaariger orders query failed for $kaarigerId", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val orders = snapshot?.documents?.mapNotNull { doc ->
                        docToOrder(doc.data ?: emptyMap(), doc.id)
                    }?.filter { normalizePhone(it.kaarigerId) == normalizedId }
                        ?.sortedByDescending { it.createdAt }
                        ?: emptyList()
                    trySend(orders)
                }
            awaitClose { listener.remove() }
        }
    }

    override fun getPendingApprovalOrders(): Flow<List<KaarigerOrder>> = ordersFlow(
        firestore.collection("kaariger_orders")
            .whereEqualTo("status", OrderStatus.PENDING_APPROVAL.name),
        sortBy = { it.deliverySubmittedAt ?: 0L }
    )

    override fun createOrder(order: KaarigerOrder): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("kaariger_orders").document(order.id).set(orderToMap(order)).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to create order"))
        }
    }

    override fun submitDelivery(
        orderId: String,
        quantity: Int,
        color: String,
        productName: String,
        notes: String?
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val doc = firestore.collection("kaariger_orders").document(orderId).get().await()
            if (!doc.exists()) {
                emit(Resource.Error("Order not found"))
                return@flow
            }
            val order = docToOrder(doc.data ?: emptyMap(), doc.id)
            if (order.status == OrderStatus.PENDING_APPROVAL) {
                emit(Resource.Error("Previous delivery is awaiting staff approval"))
                return@flow
            }
            if (order.status == OrderStatus.COMPLETED) {
                emit(Resource.Error("Order is already completed"))
                return@flow
            }
            val remaining = order.remainingQuantity()
            if (quantity <= 0) {
                emit(Resource.Error("Enter a valid quantity"))
                return@flow
            }
            if (quantity > remaining) {
                emit(Resource.Error("Cannot send more than $remaining remaining pcs"))
                return@flow
            }
            val updates = mapOf(
                "deliveredQuantity" to quantity,
                "deliveryColor" to color,
                "productName" to productName,
                "deliveryNotes" to (notes ?: ""),
                "deliverySubmittedAt" to System.currentTimeMillis(),
                "status" to OrderStatus.PENDING_APPROVAL.name,
                "rejectionReason" to ""
            )
            firestore.collection("kaariger_orders").document(orderId).update(updates).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to submit delivery"))
        }
    }

    override fun approveOrder(
        orderId: String,
        acceptedQuantity: Int,
        colorBreakdown: List<ColorQuantity>,
        rejectedQuantity: Int,
        rejectionNote: String?,
        verifiedBy: String,
        verifiedByPhone: String
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val doc = firestore.collection("kaariger_orders").document(orderId).get().await()
            if (!doc.exists()) {
                emit(Resource.Error("Order not found"))
                return@flow
            }
            val order = docToOrder(doc.data ?: emptyMap(), doc.id)
            if (order.status != OrderStatus.PENDING_APPROVAL) {
                emit(Resource.Error("No delivery pending approval"))
                return@flow
            }
            val deliveredQty = order.deliveredQuantity ?: 0
            if (deliveredQty <= 0) {
                emit(Resource.Error("Invalid delivery quantity"))
                return@flow
            }
            if (acceptedQuantity < 0 || rejectedQuantity < 0) {
                emit(Resource.Error("Quantities cannot be negative"))
                return@flow
            }
            if (acceptedQuantity + rejectedQuantity != deliveredQty) {
                emit(Resource.Error("Accepted + rejected must equal delivered ($deliveredQty)"))
                return@flow
            }
            if (acceptedQuantity > 0) {
                if (colorBreakdown.isEmpty()) {
                    emit(Resource.Error("Add at least one colour for accepted pieces"))
                    return@flow
                }
                val colorSum = colorBreakdown.sumOf { it.quantity }
                if (colorSum != acceptedQuantity) {
                    emit(Resource.Error("Colour quantities must add up to accepted ($acceptedQuantity)"))
                    return@flow
                }
                if (colorBreakdown.any { it.color.isBlank() || it.quantity <= 0 }) {
                    emit(Resource.Error("Each colour needs a name and quantity > 0"))
                    return@flow
                }
            }

            val unitPrice = order.pricePerPiece
                ?: (if (order.targetQuantity > 0) order.totalDealAmount / order.targetQuantity else 0.0)
            val now = System.currentTimeMillis()

            // Merge into inventory by SKU (name ignore-case) + colour (ignore-case)
            for (line in colorBreakdown.filter { it.quantity > 0 }) {
                val colorName = ProductColors.normalizeColor(line.color)
                val existingList = inventoryDao.getAllFinishedProducts().first()
                val existing = existingList.firstOrNull {
                    ProductColors.normalizeSku(it.name) == ProductColors.normalizeSku(order.productName) &&
                        it.color.equals(colorName, ignoreCase = true)
                }
                val product = if (existing != null) {
                    existing.copy(
                        quantity = existing.quantity + line.quantity,
                        lastUpdatedBy = verifiedBy,
                        lastUpdatedTime = now,
                        unitPrice = if (unitPrice > 0) unitPrice else existing.unitPrice,
                        orderId = order.id
                    ).toDomain()
                } else {
                    FinishedProduct(
                        id = UUID.randomUUID().toString(),
                        name = order.productName.trim(),
                        quantity = line.quantity,
                        lastUpdatedBy = verifiedBy,
                        lastUpdatedTime = now,
                        unitPrice = unitPrice.coerceAtLeast(0.0),
                        color = colorName,
                        orderId = order.id
                    )
                }
                inventoryDao.insertFinishedProduct(FinishedProductEntity.fromDomain(product))
                firestore.collection("finished_products").document(product.id).set(
                    hashMapOf(
                        "id" to product.id,
                        "name" to product.name,
                        "quantity" to product.quantity,
                        "lastUpdatedBy" to product.lastUpdatedBy,
                        "lastUpdatedTime" to product.lastUpdatedTime,
                        "unitPrice" to product.unitPrice,
                        "color" to product.color,
                        "orderId" to product.orderId,
                        "imagePath" to ""
                    )
                ).await()
            }

            val newApproved = order.approvedQuantity + acceptedQuantity
            val verifiedAt = System.currentTimeMillis()
            val isComplete = newApproved >= order.targetQuantity
            val note = rejectionNote?.trim().orEmpty()
            firestore.collection("kaariger_orders").document(orderId).update(
                mapOf(
                    "approvedQuantity" to newApproved,
                    "status" to if (isComplete) OrderStatus.COMPLETED.name else OrderStatus.ASSIGNED.name,
                    "verifiedBy" to verifiedBy,
                    "verifiedAt" to verifiedAt,
                    "deliveredQuantity" to null,
                    "deliverySubmittedAt" to null,
                    "rejectionReason" to if (rejectedQuantity > 0) {
                        note.ifBlank { "Rejected $rejectedQuantity defective pcs" }
                    } else ""
                )
            ).await()

            val breakdownStr = colorBreakdown
                .filter { it.quantity > 0 }
                .joinToString(", ") { "${it.color.trim()}:${it.quantity}" }
            val approvalRecord = OrderApprovalRecord(
                orderId = order.id,
                productName = order.productName,
                kaarigerId = order.kaarigerId,
                kaarigerName = order.kaarigerName,
                batchQuantity = acceptedQuantity,
                rejectedQuantity = rejectedQuantity,
                approvedTotalAfter = newApproved,
                targetQuantity = order.targetQuantity,
                color = breakdownStr,
                colorBreakdown = breakdownStr,
                verifiedByName = verifiedBy,
                verifiedByPhone = normalizePhone(verifiedByPhone),
                verifiedAt = verifiedAt
            )
            firestore.collection("order_approval_records").document(approvalRecord.id).set(
                mapOf(
                    "id" to approvalRecord.id,
                    "orderId" to approvalRecord.orderId,
                    "productName" to approvalRecord.productName,
                    "kaarigerId" to approvalRecord.kaarigerId,
                    "kaarigerName" to approvalRecord.kaarigerName,
                    "batchQuantity" to approvalRecord.batchQuantity,
                    "rejectedQuantity" to approvalRecord.rejectedQuantity,
                    "approvedTotalAfter" to approvalRecord.approvedTotalAfter,
                    "targetQuantity" to approvalRecord.targetQuantity,
                    "color" to approvalRecord.color,
                    "colorBreakdown" to approvalRecord.colorBreakdown,
                    "verifiedByName" to approvalRecord.verifiedByName,
                    "verifiedByPhone" to approvalRecord.verifiedByPhone,
                    "verifiedAt" to approvalRecord.verifiedAt
                )
            ).await()

            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to approve order"))
        }
    }

    override fun rejectOrder(orderId: String, verifiedBy: String, reason: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("kaariger_orders").document(orderId).update(
                mapOf(
                    "status" to OrderStatus.ASSIGNED.name,
                    "verifiedBy" to verifiedBy,
                    "verifiedAt" to System.currentTimeMillis(),
                    "rejectionReason" to reason,
                    "deliveredQuantity" to null,
                    "deliverySubmittedAt" to null
                )
            ).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to reject order"))
        }
    }

    override fun submitMaterialUsage(orderId: String, materials: List<OrderMaterial>): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val doc = firestore.collection("kaariger_orders").document(orderId).get().await()
            if (!doc.exists()) {
                emit(Resource.Error("Order not found"))
                return@flow
            }
            val order = docToOrder(doc.data ?: emptyMap(), doc.id)
            if (order.approvedQuantity < order.targetQuantity) {
                emit(Resource.Error("Complete all deliveries before reporting materials"))
                return@flow
            }
            val updatedMaterials = materials.map {
                mapOf(
                    "materialId" to it.materialId,
                    "materialName" to it.materialName,
                    "quantity" to it.quantity,
                    "unit" to it.unit,
                    "usedQuantity" to (it.usedQuantity ?: 0.0),
                    "remainingQuantity" to (it.remainingQuantity ?: 0.0)
                )
            }
            firestore.collection("kaariger_orders").document(orderId).update(
                mapOf(
                    "rawMaterials" to updatedMaterials,
                    "materialUsageReported" to true
                )
            ).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to save material usage"))
        }
    }

    override fun getPaymentsForOrder(orderId: String): Flow<List<KaarigerOrderPayment>> = paymentsFlow(
        firestore.collection("kaariger_payments").whereEqualTo("orderId", orderId)
    )

    override fun getApprovalHistoryForStaff(staffPhone: String): Flow<List<OrderApprovalRecord>> {
        val normalizedPhone = normalizePhone(staffPhone)
        return callbackFlow {
            val listener = firestore.collection("order_approval_records")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Approval history query failed for $staffPhone", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val records = snapshot?.documents?.mapNotNull { doc ->
                        docToApprovalRecord(doc.data ?: emptyMap(), doc.id)
                    }?.filter { normalizePhone(it.verifiedByPhone) == normalizedPhone }
                        ?.sortedByDescending { it.verifiedAt }
                        ?: emptyList()
                    trySend(records)
                }
            awaitClose { listener.remove() }
        }
    }

    override fun getPaymentsForKaariger(kaarigerId: String): Flow<List<KaarigerOrderPayment>> {
        val normalizedId = normalizePhone(kaarigerId)
        return callbackFlow {
            val listener = firestore.collection("kaariger_payments")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Kaariger payments query failed for $kaarigerId", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val payments = snapshot?.documents?.mapNotNull { doc ->
                        val data = doc.data ?: return@mapNotNull null
                        KaarigerOrderPayment(
                            id = data["id"] as? String ?: doc.id,
                            orderId = data["orderId"] as? String ?: "",
                            kaarigerId = data["kaarigerId"] as? String ?: "",
                            amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                            date = data["date"] as? String ?: "",
                            time = data["time"] as? String ?: "",
                            remarks = data["remarks"] as? String,
                            createdBy = data["createdBy"] as? String ?: ""
                        )
                    }?.filter { normalizePhone(it.kaarigerId) == normalizedId }
                        ?.sortedByDescending { it.date }
                        ?: emptyList()
                    trySend(payments)
                }
            awaitClose { listener.remove() }
        }
    }

    override fun addPayment(payment: KaarigerOrderPayment): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            firestore.collection("kaariger_payments").document(payment.id).set(
                hashMapOf(
                    "id" to payment.id,
                    "orderId" to payment.orderId,
                    "kaarigerId" to payment.kaarigerId,
                    "amount" to payment.amount,
                    "date" to payment.date,
                    "time" to payment.time,
                    "remarks" to (payment.remarks ?: ""),
                    "createdBy" to payment.createdBy
                )
            ).await()
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to record payment"))
        }
    }

    override fun getRepairsForKaariger(kaarigerId: String): Flow<List<OrderRepair>> {
        val normalizedId = normalizePhone(kaarigerId)
        return callbackFlow {
            val listener = firestore.collection("order_repairs")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e(TAG, "Repairs query failed for $kaarigerId", error)
                        trySend(emptyList())
                        return@addSnapshotListener
                    }
                    val list = snapshot?.documents?.mapNotNull { doc ->
                        docToRepair(doc.data ?: emptyMap(), doc.id)
                    }?.filter { normalizePhone(it.kaarigerId) == normalizedId }
                        ?.sortedByDescending { it.createdAt }
                        ?: emptyList()
                    trySend(list)
                }
            awaitClose { listener.remove() }
        }
    }

    override fun getProductCatalogNames(): Flow<List<String>> = callbackFlow {
        val registration = firestore.collection("product_catalog")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val names = snapshot?.documents
                    ?.mapNotNull { it.getString("name")?.trim()?.takeIf { n -> n.isNotEmpty() } }
                    ?.distinct()
                    ?.sorted()
                    ?: emptyList()
                trySend(names)
            }
        awaitClose { registration.remove() }
    }

    override fun createRepair(
        orderId: String,
        productName: String,
        faultyQuantity: Int,
        faultyPricePerPiece: Double,
        createdBy: String,
        notes: String?,
        kaarigerId: String,
        kaarigerName: String
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            if (faultyQuantity <= 0) {
                emit(Resource.Error("Enter a quantity greater than 0"))
                return@flow
            }
            if (productName.isBlank()) {
                emit(Resource.Error("Select a product"))
                return@flow
            }

            val standalone = orderId.isBlank() || orderId == RepairStatus.STANDALONE_ORDER_ID
            val id = UUID.randomUUID().toString()
            val faultyTotal = faultyQuantity * faultyPricePerPiece

            if (standalone) {
                if (kaarigerId.isBlank()) {
                    emit(Resource.Error("Select a kaariger"))
                    return@flow
                }
                val repair = mapOf(
                    "id" to id,
                    "orderId" to RepairStatus.STANDALONE_ORDER_ID,
                    "kaarigerId" to kaarigerId,
                    "kaarigerName" to kaarigerName,
                    "productName" to productName,
                    "faultyQuantity" to faultyQuantity,
                    "faultyPricePerPiece" to faultyPricePerPiece,
                    "faultyTotal" to faultyTotal,
                    "items" to emptyList<Map<String, Any>>(),
                    "totalRepairCost" to faultyTotal,
                    "originalDealAmount" to 0.0,
                    "dealAfterThisRepair" to 0.0,
                    "notes" to (notes ?: ""),
                    "createdBy" to createdBy,
                    "createdAt" to System.currentTimeMillis(),
                    "status" to RepairStatus.PENDING
                )
                firestore.collection("order_repairs").document(id).set(repair).await()
                emit(Resource.Success(Unit))
                return@flow
            }

            val doc = firestore.collection("kaariger_orders").document(orderId).get().await()
            if (!doc.exists()) {
                emit(Resource.Error("Order not found"))
                return@flow
            }
            val order = docToOrder(doc.data ?: emptyMap(), doc.id)
            val original = order.originalDealAmount ?: order.totalDealAmount
            // Preview only — amount is NOT deducted until admin approves in the panel.
            val dealAfterIfApproved =
                (original - order.repairDeductionTotal - faultyTotal).coerceAtLeast(0.0)
            val repair = mapOf(
                "id" to id,
                "orderId" to order.id,
                "kaarigerId" to order.kaarigerId,
                "kaarigerName" to order.kaarigerName,
                "productName" to productName,
                "faultyQuantity" to faultyQuantity,
                "faultyPricePerPiece" to faultyPricePerPiece,
                "faultyTotal" to faultyTotal,
                "items" to emptyList<Map<String, Any>>(),
                "totalRepairCost" to faultyTotal,
                "originalDealAmount" to original,
                "dealAfterThisRepair" to dealAfterIfApproved,
                "notes" to (notes ?: ""),
                "createdBy" to createdBy,
                "createdAt" to System.currentTimeMillis(),
                "status" to RepairStatus.PENDING
            )
            firestore.collection("order_repairs").document(id).set(repair).await()
            // Lock original deal early if missing, but do not touch repairDeductionTotal yet.
            if (order.originalDealAmount == null) {
                firestore.collection("kaariger_orders").document(order.id).update(
                    mapOf("originalDealAmount" to original)
                ).await()
            }
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to save repairing deduction"))
        }
    }

    override fun getRepairsForOrder(orderId: String): Flow<List<OrderRepair>> = callbackFlow {
        val listener = firestore.collection("order_repairs")
            .whereEqualTo("orderId", orderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Repairs query failed for order $orderId", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { doc ->
                    docToRepair(doc.data ?: emptyMap(), doc.id)
                }?.sortedByDescending { it.createdAt } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun docToRepair(data: Map<String, Any>, id: String): OrderRepair {
        val items = (data["items"] as? List<Map<String, Any>>)?.map {
            OrderRepairLine(
                type = it["type"] as? String ?: "",
                label = it["label"] as? String ?: "",
                quantity = (it["quantity"] as? Number)?.toInt() ?: 0,
                pricePerPiece = (it["pricePerPiece"] as? Number)?.toDouble() ?: 0.0,
                lineTotal = (it["lineTotal"] as? Number)?.toDouble() ?: 0.0
            )
        } ?: emptyList()
        return OrderRepair(
            id = data["id"] as? String ?: id,
            orderId = (data["orderId"] as? String)?.takeIf { it.isNotBlank() }
                ?: RepairStatus.STANDALONE_ORDER_ID,
            kaarigerId = data["kaarigerId"] as? String ?: "",
            kaarigerName = data["kaarigerName"] as? String ?: "",
            productName = data["productName"] as? String ?: "",
            faultyQuantity = (data["faultyQuantity"] as? Number)?.toInt() ?: 0,
            faultyPricePerPiece = (data["faultyPricePerPiece"] as? Number)?.toDouble() ?: 0.0,
            faultyTotal = (data["faultyTotal"] as? Number)?.toDouble() ?: 0.0,
            items = items,
            totalRepairCost = (data["totalRepairCost"] as? Number)?.toDouble() ?: 0.0,
            originalDealAmount = (data["originalDealAmount"] as? Number)?.toDouble() ?: 0.0,
            dealAfterThisRepair = (data["dealAfterThisRepair"] as? Number)?.toDouble() ?: 0.0,
            notes = data["notes"] as? String,
            createdBy = data["createdBy"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            // Legacy docs (no status) were already deducted → treat as APPROVED.
            status = (data["status"] as? String)?.takeIf { it.isNotBlank() } ?: RepairStatus.APPROVED,
            reviewedBy = data["reviewedBy"] as? String,
            reviewedAt = (data["reviewedAt"] as? Number)?.toLong()
        )
    }

    private fun ordersFlow(
        query: Query,
        sortBy: (KaarigerOrder) -> Long = { it.createdAt }
    ): Flow<List<KaarigerOrder>> = callbackFlow {
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Orders query failed", error)
                trySend(emptyList())
                return@addSnapshotListener
            }
            val orders = snapshot?.documents?.mapNotNull { doc ->
                docToOrder(doc.data ?: emptyMap(), doc.id)
            }?.sortedByDescending(sortBy) ?: emptyList()
            trySend(orders)
        }
        awaitClose { listener.remove() }
    }

    private fun normalizePhone(phone: String): String {
        var p = phone.trim().replace(Regex("[\\s-]"), "")
        if (p.startsWith("+91")) p = p.removePrefix("+91")
        else if (p.startsWith("91") && p.length > 10) p = p.removePrefix("91")
        // Keep last 10 digits when value is an Indian mobile with junk prefix.
        if (p.length > 10 && p.all { it.isDigit() }) p = p.takeLast(10)
        return p
    }

    private fun paymentsFlow(query: Query): Flow<List<KaarigerOrderPayment>> = callbackFlow {
        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val payments = snapshot?.documents?.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                KaarigerOrderPayment(
                    id = data["id"] as? String ?: doc.id,
                    orderId = data["orderId"] as? String ?: "",
                    kaarigerId = data["kaarigerId"] as? String ?: "",
                    amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                    date = data["date"] as? String ?: "",
                    time = data["time"] as? String ?: "",
                    remarks = data["remarks"] as? String,
                    createdBy = data["createdBy"] as? String ?: ""
                )
            } ?: emptyList()
            trySend(payments)
        }
        awaitClose { listener.remove() }
    }

    private fun orderToMap(order: KaarigerOrder): Map<String, Any?> = mapOf(
        "id" to order.id,
        "kaarigerId" to order.kaarigerId,
        "kaarigerName" to order.kaarigerName,
        "productName" to order.productName,
        "targetQuantity" to order.targetQuantity,
        "color" to order.color,
        "rawMaterials" to order.rawMaterials.map {
                mapOf(
                    "materialId" to it.materialId,
                    "materialName" to it.materialName,
                    "quantity" to it.quantity,
                    "unit" to it.unit,
                    "usedQuantity" to it.usedQuantity,
                    "remainingQuantity" to it.remainingQuantity
                )
        },
        "totalDealAmount" to order.totalDealAmount,
        "pricePerPiece" to order.pricePerPiece,
        "pricingType" to order.pricingType.name,
        "status" to order.status.name,
        "approvedQuantity" to order.approvedQuantity,
        "deliveredQuantity" to order.deliveredQuantity,
        "deliveryColor" to order.deliveryColor,
        "deliveryNotes" to order.deliveryNotes,
        "deliverySubmittedAt" to order.deliverySubmittedAt,
        "verifiedBy" to order.verifiedBy,
        "verifiedAt" to order.verifiedAt,
        "rejectionReason" to order.rejectionReason,
        "materialUsageReported" to order.materialUsageReported,
        "createdBy" to order.createdBy,
        "createdAt" to order.createdAt,
        "notes" to order.notes,
        "originalDealAmount" to order.originalDealAmount,
        "repairDeductionTotal" to order.repairDeductionTotal,
        "products" to order.products.map {
            mapOf(
                "productName" to it.productName,
                "quantity" to it.quantity,
                "pricePerPiece" to it.pricePerPiece,
                "lineTotal" to it.lineTotal
            )
        },
        "productsTotal" to order.productsTotal,
        "materialDeductions" to order.materialDeductions.map {
            mapOf(
                "type" to it.type,
                "label" to it.label,
                "quantity" to it.quantity,
                "pricePerPiece" to it.pricePerPiece,
                "lineTotal" to it.lineTotal
            )
        },
        "materialDeductionsTotal" to order.materialDeductionsTotal,
        "kharchaGiven" to order.kharchaGiven
    )

    @Suppress("UNCHECKED_CAST")
    private fun docToOrder(data: Map<String, Any>, id: String): KaarigerOrder {
        val materials = (data["rawMaterials"] as? List<Map<String, Any>>)?.map {
            OrderMaterial(
                materialId = it["materialId"] as? String ?: "",
                materialName = it["materialName"] as? String ?: "",
                quantity = (it["quantity"] as? Number)?.toDouble() ?: 0.0,
                unit = it["unit"] as? String ?: "",
                usedQuantity = (it["usedQuantity"] as? Number)?.toDouble(),
                remainingQuantity = (it["remainingQuantity"] as? Number)?.toDouble()
            )
        } ?: emptyList()
        val statusRaw = data["status"] as? String ?: OrderStatus.ASSIGNED.name
        val status = try {
            when (statusRaw) {
                "APPROVED" -> OrderStatus.COMPLETED
                else -> OrderStatus.valueOf(statusRaw)
            }
        } catch (_: Exception) {
            OrderStatus.ASSIGNED
        }
        return KaarigerOrder(
            id = data["id"] as? String ?: id,
            kaarigerId = data["kaarigerId"] as? String ?: "",
            kaarigerName = data["kaarigerName"] as? String ?: "",
            productName = data["productName"] as? String ?: "",
            targetQuantity = (data["targetQuantity"] as? Number)?.toInt() ?: 0,
            color = data["color"] as? String ?: "",
            rawMaterials = materials,
            totalDealAmount = (data["totalDealAmount"] as? Number)?.toDouble() ?: 0.0,
            pricePerPiece = (data["pricePerPiece"] as? Number)?.toDouble(),
            pricingType = try {
                OrderPricingType.valueOf(data["pricingType"] as? String ?: OrderPricingType.OVERALL.name)
            } catch (_: Exception) {
                OrderPricingType.OVERALL
            },
            status = status,
            approvedQuantity = (data["approvedQuantity"] as? Number)?.toInt() ?: 0,
            deliveredQuantity = (data["deliveredQuantity"] as? Number)?.toInt(),
            deliveryColor = data["deliveryColor"] as? String,
            deliveryNotes = data["deliveryNotes"] as? String,
            deliverySubmittedAt = (data["deliverySubmittedAt"] as? Number)?.toLong(),
            verifiedBy = data["verifiedBy"] as? String,
            verifiedAt = (data["verifiedAt"] as? Number)?.toLong(),
            rejectionReason = data["rejectionReason"] as? String,
            materialUsageReported = data["materialUsageReported"] as? Boolean ?: false,
            createdBy = data["createdBy"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: 0L,
            notes = data["notes"] as? String,
            originalDealAmount = (data["originalDealAmount"] as? Number)?.toDouble(),
            repairDeductionTotal = (data["repairDeductionTotal"] as? Number)?.toDouble() ?: 0.0,
            products = (data["products"] as? List<Map<String, Any>>)?.map {
                OrderProductLine(
                    productName = it["productName"] as? String ?: "",
                    quantity = (it["quantity"] as? Number)?.toInt() ?: 0,
                    pricePerPiece = (it["pricePerPiece"] as? Number)?.toDouble() ?: 0.0,
                    lineTotal = (it["lineTotal"] as? Number)?.toDouble() ?: 0.0
                )
            } ?: emptyList(),
            productsTotal = (data["productsTotal"] as? Number)?.toDouble() ?: 0.0,
            materialDeductions = (data["materialDeductions"] as? List<Map<String, Any>>)?.map {
                OrderRepairLine(
                    type = it["type"] as? String ?: "",
                    label = it["label"] as? String ?: "",
                    quantity = (it["quantity"] as? Number)?.toInt() ?: 0,
                    pricePerPiece = (it["pricePerPiece"] as? Number)?.toDouble() ?: 0.0,
                    lineTotal = (it["lineTotal"] as? Number)?.toDouble() ?: 0.0
                )
            } ?: emptyList(),
            materialDeductionsTotal = (data["materialDeductionsTotal"] as? Number)?.toDouble() ?: 0.0,
            kharchaGiven = (data["kharchaGiven"] as? Number)?.toDouble() ?: 0.0
        )
    }

    private fun docToApprovalRecord(data: Map<String, Any>, id: String): OrderApprovalRecord {
        return OrderApprovalRecord(
            id = data["id"] as? String ?: id,
            orderId = data["orderId"] as? String ?: "",
            productName = data["productName"] as? String ?: "",
            kaarigerId = data["kaarigerId"] as? String ?: "",
            kaarigerName = data["kaarigerName"] as? String ?: "",
            batchQuantity = (data["batchQuantity"] as? Number)?.toInt() ?: 0,
            rejectedQuantity = (data["rejectedQuantity"] as? Number)?.toInt() ?: 0,
            approvedTotalAfter = (data["approvedTotalAfter"] as? Number)?.toInt() ?: 0,
            targetQuantity = (data["targetQuantity"] as? Number)?.toInt() ?: 0,
            color = data["color"] as? String ?: "",
            colorBreakdown = data["colorBreakdown"] as? String
                ?: (data["color"] as? String ?: ""),
            verifiedByName = data["verifiedByName"] as? String ?: "",
            verifiedByPhone = data["verifiedByPhone"] as? String ?: "",
            verifiedAt = (data["verifiedAt"] as? Number)?.toLong() ?: 0L
        )
    }

    companion object {
        private const val TAG = "OrderRepository"
    }
}
