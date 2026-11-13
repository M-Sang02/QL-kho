package com.kho.glass.data

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.kho.glass.data.LocalStore.CloudWarehouseSnapshot
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

object FirestoreWarehouseSync {
    private const val INPUT_ACCOUNT_UID = "account_uid"
    private const val INPUT_STORAGE_USER_ID = "storage_user_id"
    private const val INPUT_LOCAL_WAREHOUSE_ID = "local_warehouse_id"
    private const val INPUT_REMOTE_WAREHOUSE_ID = "remote_warehouse_id"

    fun syncErrorKey(warehouseId: String): String = "warehouse_sync_error_$warehouseId"

    suspend fun createShareCode(
        warehouseId: String,
        ownerUid: String,
        requestedCode: String? = null
    ): String {
        val firestore = FirebaseFirestore.getInstance()
        val warehouse = firestore.collection("warehouses").document(warehouseId)
        val member = warehouse.collection("members").document(ownerUid)
        val warehouseSnapshot = warehouse.get(Source.SERVER).await()
        if (!warehouseSnapshot.exists()) {
            warehouse.set(
                mapOf(
                    "ownerUid" to ownerUid,
                    "schemaVersion" to 1,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            member.set(
                mapOf("role" to "owner", "joinedAt" to FieldValue.serverTimestamp())
            ).await()
        } else if (warehouseSnapshot.getString("ownerUid") == ownerUid &&
            !member.get(Source.SERVER).await().exists()
        ) {
            member.set(
                mapOf("role" to "owner", "joinedAt" to FieldValue.serverTimestamp())
            ).await()
        }
        val code = requestedCode
            ?.filterNot(Char::isWhitespace)
            ?.uppercase()
            ?: java.util.UUID.randomUUID().toString().replace("-", "").uppercase()
        require(code.length == 32 && code.all { it.isDigit() || it in 'A'..'F' }) {
            "Mã chia sẻ phải gồm đúng 32 ký tự từ A-F và 0-9."
        }
        val codeHash = sha256(code)
        return firestore.runTransaction { transaction ->
            val warehouseSnapshot = transaction.get(warehouse)
            check(warehouseSnapshot.exists() && warehouseSnapshot.getString("ownerUid") == ownerUid) {
                "Chỉ chủ kho mới có thể tạo mã chia sẻ."
            }
            val memberSnapshot = transaction.get(member)
            check(memberSnapshot.getString("role") == "owner") {
                "Tài khoản hiện tại không có quyền chia sẻ kho."
            }
            val oldCodeHash = warehouseSnapshot.getString("activeInviteHash")
            if (oldCodeHash != null && oldCodeHash != codeHash) {
                transaction.set(
                    firestore.collection("warehouseInvites").document(oldCodeHash),
                    mapOf(
                        "active" to false,
                        "disabledAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            }
            transaction.set(
                firestore.collection("warehouseInvites").document(codeHash),
                mapOf(
                    "warehouseId" to warehouseId,
                    "ownerUid" to ownerUid,
                    "active" to true,
                    "disabledAt" to FieldValue.delete(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            transaction.update(warehouse, "activeInviteHash", codeHash)
            code
        }.await()
    }

    suspend fun disableShareCode(warehouseId: String, ownerUid: String) {
        val firestore = FirebaseFirestore.getInstance()
        val warehouse = firestore.collection("warehouses").document(warehouseId)
        firestore.runTransaction { transaction ->
            val warehouseSnapshot = transaction.get(warehouse)
            check(warehouseSnapshot.exists() && warehouseSnapshot.getString("ownerUid") == ownerUid) {
                "Chỉ chủ kho mới có thể tắt mã chia sẻ."
            }
            val codeHash = warehouseSnapshot.getString("activeInviteHash")
                ?: return@runTransaction
            transaction.update(warehouse, "activeInviteHash", FieldValue.delete())
            transaction.set(
                firestore.collection("warehouseInvites").document(codeHash),
                mapOf(
                    "active" to false,
                    "disabledAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
        }.await()
    }

    suspend fun pendingShareRequests(warehouseId: String, ownerUid: String): List<String> {
        val firestore = FirebaseFirestore.getInstance()
        val warehouse = firestore.collection("warehouses").document(warehouseId)
        val warehouseSnapshot = warehouse.get(Source.SERVER).await()
        check(warehouseSnapshot.getString("ownerUid") == ownerUid) {
            "Chỉ chủ kho mới xem được yêu cầu tham gia."
        }
        return warehouse.collection("members")
            .whereEqualTo("status", "pending")
            .get(Source.SERVER)
            .await()
            .documents
            .map { it.id }
    }

    suspend fun decideShareRequest(
        warehouseId: String,
        ownerUid: String,
        memberUid: String,
        approve: Boolean
    ) {
        val firestore = FirebaseFirestore.getInstance()
        val warehouse = firestore.collection("warehouses").document(warehouseId)
        val member = warehouse.collection("members").document(memberUid)
        firestore.runTransaction { transaction ->
            val warehouseSnapshot = transaction.get(warehouse)
            val memberSnapshot = transaction.get(member)
            check(warehouseSnapshot.getString("ownerUid") == ownerUid) {
                "Chỉ chủ kho mới duyệt được yêu cầu."
            }
            check(memberSnapshot.getString("status") == "pending") {
                "Yêu cầu này không còn chờ duyệt."
            }
            transaction.update(
                member,
                mapOf(
                    "status" to if (approve) "approved" else "rejected",
                    "decidedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }

    suspend fun joinWithShareCode(code: String, memberUid: String): String {
        val normalizedCode = code.filterNot { it.isWhitespace() }.uppercase()
        require(normalizedCode.length == 32 && normalizedCode.all { it.isDigit() || it in 'A'..'F' }) {
            "Mã kho không đúng định dạng."
        }
        val firestore = FirebaseFirestore.getInstance()
        val codeHash = sha256(normalizedCode)
        val invite = firestore.collection("warehouseInvites").document(codeHash)
        val inviteSnapshot = invite.get(Source.SERVER).await()
        check(inviteSnapshot.exists() && inviteSnapshot.getBoolean("active") == true) {
            "Mã kho không tồn tại hoặc đã được thay đổi."
        }
        val warehouseId = inviteSnapshot.getString("warehouseId")
            ?: throw IllegalStateException("Mã kho không hợp lệ.")
        val warehouse = firestore.collection("warehouses").document(warehouseId)
        val membership = warehouse.collection("members").document(memberUid)
        return firestore.runTransaction { transaction ->
            val liveInvite = transaction.get(invite)
            val liveWarehouse = transaction.get(warehouse)
            check(
                liveInvite.exists() &&
                    liveInvite.getBoolean("active") == true &&
                    liveWarehouse.getString("activeInviteHash") == codeHash
            ) {
                "Mã kho đã bị thay đổi. Hãy xin chủ kho cấp mã mới."
            }
            val existingMembership = transaction.get(membership)
            if (!existingMembership.exists() ||
                existingMembership.getString("status") == "rejected" ||
                existingMembership.getString("status") == "expired"
            ) {
                transaction.set(
                    membership,
                    mapOf(
                        "role" to "member",
                        "inviteHash" to codeHash,
                        "status" to "pending",
                        "requestedAt" to FieldValue.serverTimestamp(),
                        "joinedAt" to FieldValue.serverTimestamp()
                    ),
                    com.google.firebase.firestore.SetOptions.merge()
                )
            } else if (existingMembership.getString("status") == "pending") {
                transaction.update(
                    membership,
                    mapOf(
                        "inviteHash" to codeHash,
                        "requestedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            warehouseId
        }.await()
    }

    fun schedule(
        context: Context,
        accountUid: String,
        storageUserId: String,
        localWarehouseId: String,
        remoteWarehouseId: String = localWarehouseId
    ) {
        if (accountUid.isBlank() || storageUserId.isBlank() || localWarehouseId.isBlank()) return
        val workManager = WorkManager.getInstance(context.applicationContext)
        val data = workDataOf(
            INPUT_ACCOUNT_UID to accountUid,
            INPUT_STORAGE_USER_ID to storageUserId,
            INPUT_LOCAL_WAREHOUSE_ID to localWarehouseId,
            INPUT_REMOTE_WAREHOUSE_ID to remoteWarehouseId
        )
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val name = "warehouse-sync-$accountUid-$remoteWarehouseId"
        workManager.enqueueUniqueWork(
            "$name-now",
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<WarehouseSyncWorker>()
                .setInputData(data)
                .setConstraints(constraints)
                .build()
        )
        workManager.enqueueUniquePeriodicWork(
            name,
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WarehouseSyncWorker>(15, TimeUnit.MINUTES)
                .setInputData(data)
                .setConstraints(constraints)
                .build()
        )
    }

    class WarehouseSyncWorker(
        context: Context,
        parameters: WorkerParameters
    ) : CoroutineWorker(context, parameters) {
        override suspend fun doWork(): Result {
            val uid = inputData.getString(INPUT_ACCOUNT_UID) ?: return Result.failure()
            val storageUserId = inputData.getString(INPUT_STORAGE_USER_ID) ?: return Result.failure()
            val localWarehouseId =
                inputData.getString(INPUT_LOCAL_WAREHOUSE_ID) ?: return Result.failure()
            val remoteWarehouseId =
                inputData.getString(INPUT_REMOTE_WAREHOUSE_ID) ?: return Result.failure()
            if (FirebaseApp.getApps(applicationContext).isEmpty()) return Result.failure()
            if (FirebaseAuth.getInstance().currentUser?.uid != uid) return Result.success()

            val store = LocalStore(applicationContext, storageUserId, localWarehouseId)
            return try {
                synchronize(uid, remoteWarehouseId, store)
                clearSyncError(remoteWarehouseId)
                Result.success()
            } catch (error: CancellationException) {
                throw error
            } catch (error: FirebaseFirestoreException) {
                saveSyncError(remoteWarehouseId, error.localizedMessage ?: error.code.name)
                if (
                    error.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                    error.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ||
                    error.code == FirebaseFirestoreException.Code.ABORTED
                ) {
                    Result.retry()
                } else {
                    Result.failure(workDataOf("error" to (error.localizedMessage ?: error.code.name)))
                }
            } catch (error: IOException) {
                saveSyncError(remoteWarehouseId, error.localizedMessage ?: "Lỗi kết nối mạng.")
                Result.retry()
            } catch (error: IllegalStateException) {
                saveSyncError(remoteWarehouseId, error.localizedMessage ?: "Dữ liệu đồng bộ không hợp lệ.")
                Result.failure(workDataOf("error" to (error.localizedMessage ?: "Invalid cloud data")))
            } finally {
                store.close()
            }
        }

        private fun saveSyncError(warehouseId: String, error: String) {
            applicationContext.getSharedPreferences("kho-glass-preferences", Context.MODE_PRIVATE)
                .edit()
                .putString(syncErrorKey(warehouseId), error)
                .apply()
        }

        private fun clearSyncError(warehouseId: String) {
            applicationContext.getSharedPreferences("kho-glass-preferences", Context.MODE_PRIVATE)
                .edit()
                .remove(syncErrorKey(warehouseId))
                .apply()
        }

        private suspend fun synchronize(uid: String, warehouseId: String, store: LocalStore) {
            val firestore = FirebaseFirestore.getInstance()
            val warehouse = firestore.collection("warehouses").document(warehouseId)
            val membership = warehouse.collection("members").document(uid)
            val warehouseSnapshot = warehouse.get(Source.SERVER).await()
            if (!warehouseSnapshot.exists()) {
                warehouse.set(
                    mapOf(
                        "ownerUid" to uid,
                        "schemaVersion" to 1,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                membership.set(
                    mapOf("role" to "owner", "joinedAt" to FieldValue.serverTimestamp())
                ).await()
            } else if (!membership.get(Source.SERVER).await().exists()) {
                if (warehouseSnapshot.getString("ownerUid") == uid) {
                    membership.set(
                        mapOf("role" to "owner", "joinedAt" to FieldValue.serverTimestamp())
                    ).await()
                } else {
                    throw IllegalStateException(
                        "Tài khoản này chưa được cấp quyền truy cập kho $warehouseId."
                    )
                }
            }
            val memberSnapshot = membership.get(Source.SERVER).await()
            val role = memberSnapshot.getString("role")
            val status = memberSnapshot.getString("status")
            check(role == "owner" || status == null || status == "approved") {
                if (status == "pending") {
                    "Yêu cầu tham gia đang chờ chủ kho duyệt."
                } else {
                    "Quyền truy cập kho đã bị từ chối hoặc hết hạn. Hãy nhập lại mã và chờ chủ kho duyệt."
                }
            }
            if (role != "owner" && memberSnapshot.getString("inviteHash") != null) {
                val invite = firestore.collection("warehouseInvites")
                    .document(checkNotNull(memberSnapshot.getString("inviteHash")))
                    .get(Source.SERVER)
                    .await()
                val disabledAt = invite.getTimestamp("disabledAt")?.toDate()?.time
                val lastSyncAt = memberSnapshot.getTimestamp("lastSyncAt")?.toDate()?.time
                    ?: memberSnapshot.getTimestamp("joinedAt")?.toDate()?.time
                val gracePeriodMillis = TimeUnit.DAYS.toMillis(30)
                if (
                    invite.getBoolean("active") == false &&
                    disabledAt != null &&
                    lastSyncAt != null &&
                    System.currentTimeMillis() - disabledAt >= gracePeriodMillis &&
                    System.currentTimeMillis() - lastSyncAt >= gracePeriodMillis
                ) {
                    membership.update(
                        mapOf(
                            "status" to "expired",
                            "expiredAt" to FieldValue.serverTimestamp()
                        )
                    ).await()
                    throw IllegalStateException(
                        "Mã chia sẻ đã hết hạn. Hãy nhập lại mã và chờ chủ kho duyệt."
                    )
                }
            }

            val changes = warehouse.collection("changes")
            val localRecords = store.exportCloudSnapshot().toCloudRecords()
            val existingIds = changes.get(Source.SERVER).await()
                .documents
                .mapTo(HashSet()) { it.id }
            for (chunk in localRecords.filterNot { it.documentId(uid) in existingIds }.chunked(400)) {
                val batch = firestore.batch()
                chunk.forEach { record ->
                    batch.set(
                        changes.document(record.documentId(uid)),
                        record.toDocument(uid)
                    )
                }
                batch.commit().await()
            }

            val cloudRecords = changes.get(Source.SERVER).await().documents.mapNotNull { document ->
                val type = document.getString("entityType") ?: return@mapNotNull null
                val id = document.getString("entityId") ?: return@mapNotNull null
                val payload = document.get("payload") as? Map<*, *>
                    ?: throw IllegalStateException("Bản ghi cloud ${document.id} không có payload hợp lệ.")
                val revisionAt = document.getTimestamp("revisionAt")?.toDate()?.time ?: 0L
                CloudRecord(type, id, revisionAt, payload.entries.associate { (key, value) ->
                    (key as? String
                        ?: throw IllegalStateException("Tên trường cloud không hợp lệ.")) to value
                })
            }
            store.mergeCloudSnapshot(cloudRecords.toLatestSnapshot())
            if (role != "owner") {
                membership.update("lastSyncAt", FieldValue.serverTimestamp()).await()
            }
        }
    }
}

private data class CloudRecord(
    val entityType: String,
    val entityId: String,
    val revisionAt: Long,
    val payload: Map<String, Any?>
) {
    fun documentId(uid: String): String =
        sha256("$uid|$entityType|$entityId|${canonical(payload)}")

    fun toDocument(uid: String): Map<String, Any?> = mapOf(
        "writerUid" to uid,
        "entityType" to entityType,
        "entityId" to entityId,
        "payload" to payload,
        "revisionAt" to FieldValue.serverTimestamp()
    )
}

private fun CloudWarehouseSnapshot.toCloudRecords(): List<CloudRecord> = buildList {
    products.forEach { row ->
        add(CloudRecord("products", row.id, 0L, row.toCloudMap().toMutableMap().apply {
            put("imagePath", null)
            put("additionalImagePaths", "[]")
        }))
    }
    branches.forEach { add(CloudRecord("branches", it.id, 0L, it.toCloudMap())) }
    batches.forEach { add(CloudRecord("batches", it.id, 0L, it.toCloudMap())) }
    movements.forEach { add(CloudRecord("movements", it.id, 0L, it.toCloudMap())) }
    orders.forEach { add(CloudRecord("orders", it.id, 0L, it.toCloudMap())) }
    orderLines.forEach { add(CloudRecord("orderLines", it.id, 0L, it.toCloudMap())) }
    allocations.forEach { add(CloudRecord("allocations", it.id, 0L, it.toCloudMap())) }
    payments.forEach { add(CloudRecord("payments", it.id, 0L, it.toCloudMap())) }
}

private fun Any.toCloudMap(): Map<String, Any?> =
    javaClass.declaredFields
        .filterNot { field -> field.isSynthetic || java.lang.reflect.Modifier.isStatic(field.modifiers) }
        .associate { field ->
            field.isAccessible = true
            field.name to field.get(this)
        }

private fun List<CloudRecord>.toLatestSnapshot(): CloudWarehouseSnapshot {
    val allRecords = this
    val latest = groupBy { it.entityType to it.entityId }
        .mapValues { (_, revisions) ->
            revisions.maxWithOrNull(compareBy<CloudRecord> { it.revisionAt }.thenBy {
                canonical(it.payload)
            }) ?: error("Không có phiên bản cloud hợp lệ.")
        }
        .values
    fun rows(type: String) = latest.filter { it.entityType == type }.map { it.payload }
    val permanentlyDeletedProductIds = allRecords
        .filter { it.entityType == "products" && it.payload["permanentlyDeleted"] == true }
        .mapTo(HashSet()) { it.entityId }
    val deletedOrderLineIds = allRecords
        .filter { it.entityType == "orderLines" && it.payload["productDeleted"] == true }
        .mapTo(HashSet()) { it.entityId }
    val cancelledOrderIds = allRecords
        .filter { it.entityType == "orders" && it.payload["status"] == "CANCELLED" }
        .mapTo(HashSet()) { it.entityId }
    val products = rows("products").map { data ->
        val product = ProductRecord(
            id = data.string("id"),
            name = data.string("name"),
            active = data.boolean("active"),
            createdAt = data.long("createdAt"),
            imagePath = null,
            permanentlyDeleted = data.boolean("permanentlyDeleted"),
            unitSummaryLabel = data.string("unitSummaryLabel"),
            additionalImagePaths = "[]"
        )
        if (product.id in permanentlyDeletedProductIds) {
            product.copy(active = false, permanentlyDeleted = true)
        } else {
            product
        }
    }
    val branches = rows("branches").map { data ->
        UnitBranchRecord(
            id = data.string("id"),
            productId = data.string("productId"),
            name = data.string("name"),
            unitType = data.string("unitType"),
            stock = data.double("stock"),
            buyPrice = data.long("buyPrice"),
            sellPrice = data.long("sellPrice"),
            lowStockThreshold = data.double("lowStockThreshold")
        )
    }
    val batches = rows("batches").map { data ->
        StockBatchRecord(
            id = data.string("id"),
            branchId = data.string("branchId"),
            receivedAt = data.long("receivedAt"),
            quantityReceived = data.double("quantityReceived"),
            quantityRemaining = data.double("quantityRemaining"),
            unitCost = data.long("unitCost"),
            approvalStatus = data.string("approvalStatus")
        )
    }
    val movements = rows("movements").map { data ->
        StockMovementRecord(
            id = data.string("id"),
            branchId = data.string("branchId"),
            type = data.string("type"),
            quantityDelta = data.double("quantityDelta"),
            happenedAt = data.long("happenedAt"),
            referenceId = data.nullableString("referenceId"),
            syncStatus = data.string("syncStatus")
        )
    }
    val paymentTotals = rows("payments")
        .groupBy { it.string("orderId") }
        .mapValues { (_, payments) ->
            payments.filter { it.string("kind") == "PAYMENT" }
                .sumOf { it.long("amount") }
        }
    val refundTotals = rows("payments")
        .groupBy { it.string("orderId") }
        .mapValues { (_, payments) ->
            payments.filter { it.string("kind") == "REFUND" }
                .sumOf { it.long("amount") }
        }
    val cancelledRefundTotals = allRecords
        .filter { it.entityType == "orders" && it.entityId in cancelledOrderIds }
        .groupBy { it.entityId }
        .mapValues { (_, revisions) ->
            revisions.mapNotNull { it.payload["refundPending"] as? Number }
                .maxOfOrNull { it.toLong() } ?: 0L
        }
    val orders = rows("orders").map { data ->
        val order = SalesOrderRecord(
            id = data.string("id"),
            customerName = data.string("customerName"),
            createdAt = data.long("createdAt"),
            completedAt = data.nullableLong("completedAt"),
            status = data.string("status"),
            totalAmount = data.long("totalAmount"),
            paidAmount = data.long("paidAmount"),
            refundPending = data.long("refundPending")
        )
        val paidAmount = paymentTotals[order.id] ?: order.paidAmount
        if (order.id in cancelledOrderIds) {
            order.copy(
                status = "CANCELLED",
                paidAmount = paidAmount,
                refundPending = (
                    (cancelledRefundTotals[order.id] ?: order.refundPending) -
                        (refundTotals[order.id] ?: 0L)
                    ).coerceAtLeast(0L)
            )
        } else {
            val complete = order.totalAmount > 0L && paidAmount >= order.totalAmount
            order.copy(
                paidAmount = paidAmount,
                status = when {
                    complete -> "COMPLETED"
                    paidAmount > 0L -> "PARTIALLY_PAID"
                    else -> "UNPAID"
                },
                completedAt = if (complete) {
                    order.completedAt ?: rows("payments")
                        .filter { it.string("orderId") == order.id && it.string("kind") == "PAYMENT" }
                        .maxOfOrNull { it.long("happenedAt") }
                } else {
                    null
                }
            )
        }
    }
    val orderLines = rows("orderLines").map { data ->
        val line = SalesOrderLineRecord(
            id = data.string("id"),
            orderId = data.string("orderId"),
            branchId = data.string("branchId"),
            productName = data.string("productName"),
            branchName = data.string("branchName"),
            quantity = data.double("quantity"),
            unitPrice = data.long("unitPrice"),
            lineTotal = data.long("lineTotal"),
            position = data.int("position"),
            productDeleted = data.boolean("productDeleted")
        )
        if (line.id in deletedOrderLineIds) line.copy(productDeleted = true) else line
    }
    val allocations = rows("allocations").map { data ->
        OrderLineBatchAllocationRecord(
            id = data.string("id"),
            orderLineId = data.string("orderLineId"),
            batchId = data.string("batchId"),
            quantity = data.double("quantity"),
            unitCost = data.long("unitCost")
        )
    }
    val payments = rows("payments").map { data ->
        OrderPaymentRecord(
            id = data.string("id"),
            orderId = data.string("orderId"),
            amount = data.long("amount"),
            happenedAt = data.long("happenedAt"),
            kind = data.string("kind")
        )
    }
    return CloudWarehouseSnapshot(
        products, branches, batches, movements, orders, orderLines, allocations, payments
    )
}

private fun Map<String, Any?>.string(key: String): String =
    this[key] as? String ?: throw IllegalStateException("Bản ghi cloud thiếu trường $key.")

private fun Map<String, Any?>.nullableString(key: String): String? =
    this[key] as? String

private fun Map<String, Any?>.number(key: String): Number =
    this[key] as? Number ?: throw IllegalStateException("Bản ghi cloud thiếu trường $key.")

private fun Map<String, Any?>.long(key: String): Long = number(key).toLong()
private fun Map<String, Any?>.int(key: String): Int = number(key).toInt()
private fun Map<String, Any?>.double(key: String): Double = number(key).toDouble()

private fun Map<String, Any?>.nullableLong(key: String): Long? =
    (this[key] as? Number)?.toLong()

private fun Map<String, Any?>.boolean(key: String): Boolean =
    this[key] as? Boolean ?: throw IllegalStateException("Bản ghi cloud thiếu trường $key.")

private fun canonical(value: Any?): String = when (value) {
    null -> "null"
    is Map<*, *> -> value.entries
        .sortedBy { it.key.toString() }
        .joinToString(prefix = "{", postfix = "}") {
            "${it.key}:${canonical(it.value)}"
        }
    is Iterable<*> -> value.joinToString(prefix = "[", postfix = "]") { canonical(it) }
    is String -> "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""
    else -> value.toString()
}

private fun sha256(value: String): String =
    MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
