package com.laiza.worker.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.domain.models.DeliveryPartner
import com.laiza.worker.domain.models.DeliveryPartnerDefaults
import com.laiza.worker.domain.models.EcommercePlatform
import com.laiza.worker.domain.models.PickupLineItem
import com.laiza.worker.domain.models.PickupRecord
import com.laiza.worker.domain.models.ReturnRecord
import com.laiza.worker.domain.models.ReturnType
import com.laiza.worker.domain.repository.StoreOperationsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoreOperationsRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : StoreOperationsRepository {

    override fun getAllPickups(): Flow<List<PickupRecord>> = callbackFlow {
        val listener = firestore.collection("pickup_records")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                val records = snapshot?.documents?.mapNotNull { doc ->
                    parsePickup(doc.id, doc.data ?: return@mapNotNull null)
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
                    parseReturn(doc.id, doc.data ?: return@mapNotNull null)
                } ?: emptyList()
                trySend(records)
            }
        awaitClose { listener.remove() }
    }

    override fun getDeliveryPartners(): Flow<List<DeliveryPartner>> = callbackFlow {
        val col = firestore.collection("delivery_partners")
        val listener = col
            .orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                val fromDb = snapshot?.documents?.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    val name = (d["name"] as? String)?.trim().orEmpty()
                    if (name.isBlank()) return@mapNotNull null
                    DeliveryPartner(
                        id = d["id"] as? String ?: doc.id,
                        name = name,
                        createdAt = (d["createdAt"] as? Number)?.toLong() ?: 0L
                    )
                } ?: emptyList()

                // Merge defaults so staff always see common couriers even before seed
                val names = linkedSetOf<String>()
                val merged = mutableListOf<DeliveryPartner>()
                DeliveryPartnerDefaults.ALL.forEach { def ->
                    if (names.add(def.lowercase())) {
                        val existing = fromDb.find { it.name.equals(def, ignoreCase = true) }
                        merged.add(existing ?: DeliveryPartner(id = "default_${def.lowercase()}", name = def, createdAt = 0L))
                    }
                }
                fromDb.forEach { p ->
                    if (names.add(p.name.lowercase())) merged.add(p)
                }
                trySend(merged.sortedBy { it.name.lowercase() })
            }
        awaitClose { listener.remove() }
    }

    override fun addDeliveryPartner(name: String): Flow<Resource<DeliveryPartner>> = flow {
        emit(Resource.Loading())
        try {
            val trimmed = name.trim()
            if (trimmed.isBlank()) {
                emit(Resource.Error("Enter a partner name"))
                return@flow
            }
            val existing = firestore.collection("delivery_partners")
                .get()
                .await()
                .documents
                .any {
                    (it.getString("name") ?: "").equals(trimmed, ignoreCase = true)
                }
            if (existing || DeliveryPartnerDefaults.ALL.any { it.equals(trimmed, ignoreCase = true) }) {
                emit(Resource.Success(DeliveryPartner(name = trimmed)))
                return@flow
            }
            val partner = DeliveryPartner(id = UUID.randomUUID().toString(), name = trimmed)
            firestore.collection("delivery_partners").document(partner.id)
                .set(
                    mapOf(
                        "id" to partner.id,
                        "name" to partner.name,
                        "createdAt" to partner.createdAt
                    )
                )
                .await()
            emit(Resource.Success(partner))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to add partner"))
        }
    }

    override fun recordPickup(record: PickupRecord): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            if (record.quantity <= 0) {
                emit(Resource.Error("Enter a valid quantity"))
                return@flow
            }
            firestore.collection("pickup_records").document(record.id)
                .set(pickupToMap(record))
                .await()
            emit(Resource.Success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to record pickup"))
        }
    }

    override fun recordReturn(record: ReturnRecord): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            if (record.quantity <= 0) {
                emit(Resource.Error("Enter a valid quantity"))
                return@flow
            }
            firestore.collection("return_records").document(record.id)
                .set(returnToMap(record))
                .await()
            emit(Resource.Success(Unit))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to record return"))
        }
    }

    private fun parsePickup(docId: String, d: Map<String, Any>): PickupRecord {
        @Suppress("UNCHECKED_CAST")
        val rawItems = d["items"] as? List<Map<String, Any>>
        val items = rawItems?.mapNotNull { m ->
            val name = m["productName"] as? String ?: return@mapNotNull null
            PickupLineItem(
                productId = m["productId"] as? String ?: "",
                productName = name,
                color = m["color"] as? String ?: "",
                quantity = (m["quantity"] as? Number)?.toInt() ?: 0
            )
        } ?: emptyList()

        val productId = d["productId"] as? String ?: items.firstOrNull()?.productId.orEmpty()
        val productName = d["productName"] as? String ?: items.firstOrNull()?.productName.orEmpty()
        val color = d["color"] as? String ?: items.firstOrNull()?.color.orEmpty()
        val quantity = (d["quantity"] as? Number)?.toInt()
            ?: items.sumOf { it.quantity }

        return PickupRecord(
            id = d["id"] as? String ?: docId,
            items = items,
            productId = productId,
            productName = productName,
            color = color,
            quantity = quantity,
            partner = EcommercePlatform.normalize(d["partner"] as? String),
            deliveryPartner = (d["deliveryPartner"] as? String)?.trim().orEmpty(),
            staffId = d["staffId"] as? String ?: "",
            staffName = d["staffName"] as? String ?: "",
            date = d["date"] as? String ?: "",
            time = d["time"] as? String ?: "",
            timestamp = (d["timestamp"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun parseReturn(docId: String, d: Map<String, Any>): ReturnRecord {
        return ReturnRecord(
            id = d["id"] as? String ?: docId,
            productId = d["productId"] as? String ?: "",
            productName = d["productName"] as? String ?: "",
            color = d["color"] as? String ?: "",
            quantity = (d["quantity"] as? Number)?.toInt() ?: 0,
            partner = EcommercePlatform.normalize(d["partner"] as? String),
            deliveryPartner = (d["deliveryPartner"] as? String)?.trim().orEmpty(),
            returnType = ReturnType.fromString(d["returnType"] as? String ?: "RTO"),
            staffId = d["staffId"] as? String ?: "",
            staffName = d["staffName"] as? String ?: "",
            date = d["date"] as? String ?: "",
            time = d["time"] as? String ?: "",
            notes = d["notes"] as? String,
            timestamp = (d["timestamp"] as? Number)?.toLong() ?: 0L
        )
    }

    private fun pickupToMap(r: PickupRecord): Map<String, Any> {
        val lines = r.lineItems
        val first = lines.firstOrNull()
        return mapOf(
            "id" to r.id,
            "items" to lines.map {
                mapOf(
                    "productId" to it.productId,
                    "productName" to it.productName,
                    "color" to it.color,
                    "quantity" to it.quantity
                )
            },
            "productId" to (first?.productId ?: r.productId),
            "productName" to (first?.productName ?: r.productName),
            "color" to (first?.color ?: r.color),
            "quantity" to lines.sumOf { it.quantity }.let { if (it > 0) it else r.quantity },
            "partner" to r.partner,
            "deliveryPartner" to r.deliveryPartner,
            "staffId" to r.staffId,
            "staffName" to r.staffName,
            "date" to r.date,
            "time" to r.time,
            "timestamp" to r.timestamp
        )
    }

    private fun returnToMap(r: ReturnRecord) = mapOf(
        "id" to r.id,
        "productId" to r.productId,
        "productName" to r.productName,
        "color" to r.color,
        "quantity" to r.quantity,
        "partner" to r.partner,
        "deliveryPartner" to r.deliveryPartner,
        "returnType" to r.returnType.name,
        "staffId" to r.staffId,
        "staffName" to r.staffName,
        "date" to r.date,
        "time" to r.time,
        "notes" to (r.notes ?: ""),
        "timestamp" to r.timestamp
    )
}
