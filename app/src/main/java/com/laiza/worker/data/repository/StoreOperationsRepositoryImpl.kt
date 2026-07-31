package com.laiza.worker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.EcommercePartner
import com.laiza.worker.domain.models.PickupRecord
import com.laiza.worker.domain.models.ReturnRecord
import com.laiza.worker.domain.models.ReturnType
import com.laiza.worker.domain.repository.InventoryRepository
import com.laiza.worker.domain.repository.StoreOperationsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreOperationsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val inventoryRepository: InventoryRepository
) : StoreOperationsRepository {

    override fun getAllPickups(): Flow<List<PickupRecord>> = callbackFlow {
        val listener = firestore.collection("pickup_records")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val records = snapshot?.documents?.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    PickupRecord(
                        id = d["id"] as? String ?: doc.id,
                        productId = d["productId"] as? String ?: "",
                        productName = d["productName"] as? String ?: "",
                        color = d["color"] as? String ?: "",
                        quantity = (d["quantity"] as? Number)?.toInt() ?: 0,
                        partner = EcommercePartner.fromString(d["partner"] as? String ?: ""),
                        staffId = d["staffId"] as? String ?: "",
                        staffName = d["staffName"] as? String ?: "",
                        date = d["date"] as? String ?: "",
                        time = d["time"] as? String ?: "",
                        timestamp = (d["timestamp"] as? Number)?.toLong() ?: 0L
                    )
                } ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    override fun getAllReturns(): Flow<List<ReturnRecord>> = callbackFlow {
        val listener = firestore.collection("return_records")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val records = snapshot?.documents?.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    ReturnRecord(
                        id = d["id"] as? String ?: doc.id,
                        productId = d["productId"] as? String ?: "",
                        productName = d["productName"] as? String ?: "",
                        color = d["color"] as? String ?: "",
                        quantity = (d["quantity"] as? Number)?.toInt() ?: 0,
                        partner = EcommercePartner.fromString(d["partner"] as? String ?: ""),
                        returnType = ReturnType.fromString(d["returnType"] as? String ?: "RTO"),
                        staffId = d["staffId"] as? String ?: "",
                        staffName = d["staffName"] as? String ?: "",
                        date = d["date"] as? String ?: "",
                        time = d["time"] as? String ?: "",
                        notes = d["notes"] as? String,
                        timestamp = (d["timestamp"] as? Number)?.toLong() ?: 0L
                    )
                } ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    override fun recordPickup(record: PickupRecord): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val adjust = inventoryRepository
                .adjustFinishedProductQuantity(record.productId, -record.quantity)
                .filter { it !is Resource.Loading }
                .first()

            when (adjust) {
                is Resource.Success -> {
                    firestore.collection("pickup_records").document(record.id)
                        .set(pickupToMap(record))
                        .await()
                    emit(Resource.Success(Unit))
                }
                is Resource.Error -> emit(Resource.Error(adjust.message ?: "Failed to deduct inventory"))
                else -> emit(Resource.Error("Failed to deduct inventory"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to record pickup"))
        }
    }

    override fun recordReturn(record: ReturnRecord): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val adjust = inventoryRepository
                .adjustFinishedProductQuantity(record.productId, record.quantity)
                .filter { it !is Resource.Loading }
                .first()

            when (adjust) {
                is Resource.Success -> {
                    firestore.collection("return_records").document(record.id)
                        .set(returnToMap(record))
                        .await()
                    emit(Resource.Success(Unit))
                }
                is Resource.Error -> emit(Resource.Error(adjust.message ?: "Failed to restock inventory"))
                else -> emit(Resource.Error("Failed to restock inventory"))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to record return"))
        }
    }

    private fun pickupToMap(r: PickupRecord) = mapOf(
        "id" to r.id, "productId" to r.productId, "productName" to r.productName,
        "color" to r.color, "quantity" to r.quantity, "partner" to r.partner.name,
        "staffId" to r.staffId, "staffName" to r.staffName,
        "date" to r.date, "time" to r.time, "timestamp" to r.timestamp
    )

    private fun returnToMap(r: ReturnRecord) = mapOf(
        "id" to r.id, "productId" to r.productId, "productName" to r.productName,
        "color" to r.color, "quantity" to r.quantity, "partner" to r.partner.name,
        "returnType" to r.returnType.name, "staffId" to r.staffId, "staffName" to r.staffName,
        "date" to r.date, "time" to r.time, "notes" to (r.notes ?: ""), "timestamp" to r.timestamp
    )
}
