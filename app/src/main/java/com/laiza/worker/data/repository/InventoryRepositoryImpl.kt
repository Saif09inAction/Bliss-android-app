package com.laiza.worker.data.repository

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.laiza.worker.core.utils.Resource
import com.laiza.worker.core.utils.FirebaseStorageHelper
import com.laiza.worker.core.local.dao.InventoryDao
import com.laiza.worker.core.local.entity.FinishedProductComponentEntity
import com.laiza.worker.core.local.entity.FinishedProductEntity
import com.laiza.worker.core.local.entity.RawMaterialEntity
import com.laiza.worker.domain.models.FinishedProduct
import com.laiza.worker.domain.models.RawMaterial
import com.laiza.worker.domain.models.RawMaterialConsumption
import com.laiza.worker.domain.repository.InventoryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class InventoryRepositoryImpl @Inject constructor(
    private val inventoryDao: InventoryDao,
    private val firestore: FirebaseFirestore,
    private val storageHelper: FirebaseStorageHelper,
    @ApplicationContext private val context: Context
) : InventoryRepository {

    private val repositoryScope = CoroutineScope(Dispatchers.IO + kotlinx.coroutines.SupervisorJob())

    override fun getAllRawMaterials(): Flow<List<RawMaterial>> {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = fetchRawMaterialsFromFirestore()
                for (item in list) {
                    inventoryDao.insertRawMaterial(RawMaterialEntity.fromDomain(item))
                }
            } catch (e: Exception) {
                // Ignore sync fail
            }
        }
        return inventoryDao.getAllRawMaterials().map { list -> list.map { it.toDomain() } }
    }

    override fun addRawMaterial(material: RawMaterial): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Write to Room instantly
            inventoryDao.insertRawMaterial(RawMaterialEntity.fromDomain(material))
            emit(Resource.Success(Unit))

            // 2. Offload Cloud sync
            repositoryScope.launch {
                var finalMaterial = material
                val localPath = material.imagePath
                if (!localPath.isNullOrBlank() && !localPath.startsWith("http")) {
                    try {
                        val downloadUrl = storageHelper.uploadImage(context, localPath, "raw_materials")
                        if (downloadUrl != null) {
                            finalMaterial = material.copy(imagePath = downloadUrl)
                            inventoryDao.insertRawMaterial(RawMaterialEntity.fromDomain(finalMaterial))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                try {
                    saveRawMaterialToFirestore(finalMaterial)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to add raw material"))
        }
    }

    override fun updateRawMaterial(material: RawMaterial): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Update Room instantly
            inventoryDao.updateRawMaterial(RawMaterialEntity.fromDomain(material))
            emit(Resource.Success(Unit))

            // 2. Offload Cloud sync
            repositoryScope.launch {
                var finalMaterial = material
                val localPath = material.imagePath
                if (!localPath.isNullOrBlank() && !localPath.startsWith("http")) {
                    try {
                        val downloadUrl = storageHelper.uploadImage(context, localPath, "raw_materials")
                        if (downloadUrl != null) {
                            finalMaterial = material.copy(imagePath = downloadUrl)
                            inventoryDao.updateRawMaterial(RawMaterialEntity.fromDomain(finalMaterial))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                try {
                    saveRawMaterialToFirestore(finalMaterial)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update raw material"))
        }
    }

    override fun deleteRawMaterial(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            try {
                deleteRawMaterialFromFirestore(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            inventoryDao.deleteRawMaterialById(id)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to delete raw material"))
        }
    }

    override fun getAllFinishedProducts(): Flow<List<FinishedProduct>> {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val list = fetchFinishedProductsFromFirestore()
                for (item in list) {
                    inventoryDao.insertFinishedProduct(FinishedProductEntity.fromDomain(item))
                }
            } catch (e: Exception) {
                // Ignore sync fail
            }
        }
        return inventoryDao.getAllFinishedProducts().map { list -> list.map { it.toDomain() } }
    }

    override fun saveFinishedProduct(
        product: FinishedProduct,
        rawMaterialsUsed: List<RawMaterialConsumption>
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Save to Room database instantly to update UI
            val productEntity = FinishedProductEntity.fromDomain(product)
            val componentEntities = rawMaterialsUsed.map { consumption ->
                FinishedProductComponentEntity(
                    id = UUID.randomUUID().toString(),
                    finishedProductId = product.id,
                    rawMaterialId = consumption.rawMaterialId,
                    quantityUsed = consumption.quantityUsed
                )
            }
            inventoryDao.assembleFinishedProduct(productEntity, componentEntities)
            emit(Resource.Success(Unit))

            // 2. Offload Firestore upload & image storage to background
            repositoryScope.launch {
                var finalProduct = product
                val localPath = product.imagePath
                if (!localPath.isNullOrBlank() && !localPath.startsWith("http")) {
                    try {
                        val downloadUrl = storageHelper.uploadImage(context, localPath, "finished_products")
                        if (downloadUrl != null) {
                            finalProduct = product.copy(imagePath = downloadUrl)
                            // Update Room DB cache with cloud URL
                            val finalProductEntity = FinishedProductEntity.fromDomain(finalProduct)
                            inventoryDao.insertFinishedProduct(finalProductEntity)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                try {
                    // Save finished product to firestore
                    saveFinishedProductToFirestore(finalProduct)

                    // Deduct raw material levels in cloud (multiplying by quantity produced!)
                    for (consumption in rawMaterialsUsed) {
                        deductRawMaterialStockInFirestore(consumption.rawMaterialId, consumption.quantityUsed * finalProduct.quantity)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to assemble finished product"))
        }
    }

    override fun deleteFinishedProduct(id: String): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // Get current product state from Room to know quantity remaining
            val product = inventoryDao.getFinishedProductById(id)
            if (product != null) {
                // Get components recipe for this product
                val components = inventoryDao.getComponentsForProductOnce(id)
                for (component in components) {
                    val refillQty = component.quantityUsed * product.quantity
                    val rawMat = inventoryDao.getRawMaterialById(component.rawMaterialId)
                    if (rawMat != null) {
                        val newQty = rawMat.quantity + refillQty
                        inventoryDao.insertRawMaterial(rawMat.copy(quantity = newQty))
                        // Update cloud raw materials stock
                        try {
                            refillRawMaterialStockInFirestore(component.rawMaterialId, refillQty)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            try {
                deleteFinishedProductFromFirestore(id)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            inventoryDao.deleteFinishedProductById(id)
            emit(Resource.Success(Unit))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to delete finished product"))
        }
    }

    override fun adjustFinishedProductQuantity(productId: String, delta: Int): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            val existing = inventoryDao.getFinishedProductById(productId)
                ?: run {
                    emit(Resource.Error("Product not found in inventory"))
                    return@flow
                }
            val newQty = existing.quantity + delta
            if (newQty < 0) {
                emit(Resource.Error("Insufficient stock. Available: ${existing.quantity}"))
                return@flow
            }
            inventoryDao.adjustFinishedProductQuantity(productId, delta)
            val updated = existing.copy(quantity = newQty, lastUpdatedTime = System.currentTimeMillis())
            inventoryDao.insertFinishedProduct(updated)
            emit(Resource.Success(Unit))
            repositoryScope.launch {
                try {
                    firestore.collection("finished_products").document(productId)
                        .update("quantity", newQty, "lastUpdatedTime", System.currentTimeMillis())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Failed to update inventory"))
        }
    }

    private suspend fun fetchRawMaterialsFromFirestore(): List<RawMaterial> = suspendCancellableCoroutine { continuation ->
        firestore.collection("raw_materials").get()
            .addOnSuccessListener { querySnapshot ->
                val list = querySnapshot.map { doc ->
                    RawMaterial(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        quantity = doc.getDouble("quantity") ?: 0.0,
                        unit = doc.getString("unit") ?: "units",
                        minimumStock = doc.getDouble("minimumStock") ?: 0.0,
                        supplier = doc.getString("supplier") ?: "",
                        lastUpdatedBy = doc.getString("lastUpdatedBy") ?: "",
                        lastUpdatedTime = doc.getLong("lastUpdatedTime") ?: 0L,
                        imagePath = doc.getString("imagePath")
                    )
                }
                continuation.resume(list)
            }
            .addOnFailureListener { err -> continuation.resumeWithException(err) }
    }

    private suspend fun saveRawMaterialToFirestore(material: RawMaterial): Unit = suspendCancellableCoroutine { continuation ->
        val data = hashMapOf(
            "id" to material.id,
            "name" to material.name,
            "quantity" to material.quantity,
            "unit" to material.unit,
            "minimumStock" to material.minimumStock,
            "supplier" to material.supplier,
            "lastUpdatedBy" to material.lastUpdatedBy,
            "lastUpdatedTime" to material.lastUpdatedTime,
            "imagePath" to (material.imagePath ?: "")
        )
        firestore.collection("raw_materials").document(material.id).set(data)
            .addOnSuccessListener { continuation.resume(Unit) }
            .addOnFailureListener { err -> continuation.resumeWithException(err) }
    }

    private suspend fun deleteRawMaterialFromFirestore(id: String): Unit = suspendCancellableCoroutine { continuation ->
        firestore.collection("raw_materials").document(id).delete()
            .addOnSuccessListener { continuation.resume(Unit) }
            .addOnFailureListener { err -> continuation.resumeWithException(err) }
    }

    private suspend fun fetchFinishedProductsFromFirestore(): List<FinishedProduct> = suspendCancellableCoroutine { continuation ->
        firestore.collection("finished_products").get()
            .addOnSuccessListener { querySnapshot ->
                val list = querySnapshot.map { doc ->
                    FinishedProduct(
                        id = doc.getString("id") ?: doc.id,
                        name = doc.getString("name") ?: "",
                        quantity = doc.getLong("quantity")?.toInt() ?: 0,
                        lastUpdatedBy = doc.getString("lastUpdatedBy") ?: "",
                        lastUpdatedTime = doc.getLong("lastUpdatedTime") ?: 0L,
                        imagePath = doc.getString("imagePath"),
                        unitPrice = doc.getDouble("unitPrice") ?: 0.0,
                        color = doc.getString("color") ?: "",
                        orderId = doc.getString("orderId")
                    )
                }
                continuation.resume(list)
            }
            .addOnFailureListener { err -> continuation.resumeWithException(err) }
    }

    private suspend fun saveFinishedProductToFirestore(product: FinishedProduct): Unit = suspendCancellableCoroutine { continuation ->
        val data = hashMapOf(
            "id" to product.id,
            "name" to product.name,
            "quantity" to product.quantity,
            "lastUpdatedBy" to product.lastUpdatedBy,
            "lastUpdatedTime" to product.lastUpdatedTime,
            "imagePath" to (product.imagePath ?: ""),
            "unitPrice" to product.unitPrice,
            "color" to product.color,
            "orderId" to product.orderId
        )
        firestore.collection("finished_products").document(product.id).set(data)
            .addOnSuccessListener { continuation.resume(Unit) }
            .addOnFailureListener { err -> continuation.resumeWithException(err) }
    }

    private suspend fun deleteFinishedProductFromFirestore(id: String): Unit = suspendCancellableCoroutine { continuation ->
        firestore.collection("finished_products").document(id).delete()
            .addOnSuccessListener { continuation.resume(Unit) }
            .addOnFailureListener { err -> continuation.resumeWithException(err) }
    }

    private suspend fun deductRawMaterialStockInFirestore(rawMaterialId: String, qty: Double): Unit = suspendCancellableCoroutine { continuation ->
        val docRef = firestore.collection("raw_materials").document(rawMaterialId)
        docRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val currentQty = snapshot.getDouble("quantity") ?: 0.0
                val newQty = (currentQty - qty).coerceAtLeast(0.0)
                docRef.update("quantity", newQty)
                    .addOnSuccessListener { continuation.resume(Unit) }
                    .addOnFailureListener { err -> continuation.resumeWithException(err) }
            } else {
                continuation.resume(Unit)
            }
        }.addOnFailureListener { err -> continuation.resumeWithException(err) }
    }

    private suspend fun refillRawMaterialStockInFirestore(rawMaterialId: String, qty: Double): Unit = suspendCancellableCoroutine { continuation ->
        val docRef = firestore.collection("raw_materials").document(rawMaterialId)
        docRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val currentQty = snapshot.getDouble("quantity") ?: 0.0
                val newQty = currentQty + qty
                docRef.update("quantity", newQty)
                    .addOnSuccessListener { continuation.resume(Unit) }
                    .addOnFailureListener { err -> continuation.resumeWithException(err) }
            } else {
                continuation.resume(Unit)
            }
        }.addOnFailureListener { err -> continuation.resumeWithException(err) }
    }
}
