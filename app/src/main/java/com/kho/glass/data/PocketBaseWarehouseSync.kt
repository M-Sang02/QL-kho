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
import com.kho.glass.data.LocalStore.CloudWarehouseSnapshot
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.URLEncoder
import java.security.MessageDigest
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit
import org.json.JSONArray
import org.json.JSONObject

object PocketBaseWarehouseSync {
    private const val INPUT_ACCOUNT_UID = "account_uid"
    private const val INPUT_STORAGE_USER_ID = "storage_user_id"
    private const val INPUT_LOCAL_WAREHOUSE_ID = "local_warehouse_id"
    private const val INPUT_REMOTE_WAREHOUSE_ID = "remote_warehouse_id"
    private const val WAREHOUSES = "warehouses"
    private const val MEMBERS = "warehouse_members"
    private const val INVITES = "warehouse_invites"
    private const val CHANGES = "warehouse_changes"

    fun syncErrorKey(warehouseId: String): String = "warehouse_sync_error_$warehouseId"

    suspend fun createShareCode(
        warehouseId: String,
        ownerUid: String,
        requestedCode: String? = null
    ): String {
        val warehouse = getWarehouse(warehouseId)
        if (warehouse == null) {
            createRecord(
                WAREHOUSES,
                JSONObject()
                    .put("warehouseKey", warehouseId)
                    .put("ownerUid", ownerUid)
                    .put("schemaVersion", 1)
                    .put("createdAt", System.currentTimeMillis())
            )
        } else {
            check(warehouse.optString("ownerUid") == ownerUid) {
                "Chỉ chủ kho mới có thể tạo mã chia sẻ."
            }
        }
        val membership = getMember(warehouseId, ownerUid)
        if (membership == null) {
            createMember(
                warehouseId,
                ownerUid,
                JSONObject()
                    .put("role", "owner")
                    .put("status", "approved")
                    .put("joinedAt", System.currentTimeMillis())
            )
        } else {
            check(membership.optString("role") == "owner") {
                "Tài khoản hiện tại không có quyền chia sẻ kho."
            }
        }
        val code = requestedCode
            ?.filterNot(Char::isWhitespace)
            ?.uppercase()
            ?: java.util.UUID.randomUUID().toString().replace("-", "").uppercase()
        require(code.length == 32 && code.all { it.isDigit() || it in 'A'..'F' }) {
            "Mã chia sẻ phải gồm đúng 32 ký tự từ A-F và 0-9."
        }
        val codeHash = sha256(code)
        val now = System.currentTimeMillis()
        val invite = getByField(INVITES, "codeHash", codeHash)
        val inviteFields = JSONObject()
            .put("codeHash", codeHash)
            .put("warehouseKey", warehouseId)
            .put("warehouse", checkNotNull(getWarehouse(warehouseId)).getString("id"))
            .put("ownerUid", ownerUid)
            .put("active", true)
            .put("disabledAt", JSONObject.NULL)
            .put("updatedAt", now)
        if (invite == null) {
            createRecord(INVITES, inviteFields.put("createdAt", now))
        } else {
            updateRecord(INVITES, invite.getString("id"), inviteFields)
        }
        val currentWarehouse = checkNotNull(getWarehouse(warehouseId))
        val previousHash = currentWarehouse.optString("activeInviteHash").takeIf { it.isNotBlank() }
        if (previousHash != null && previousHash != codeHash) {
            getByField(INVITES, "codeHash", previousHash)?.let { oldInvite ->
                updateRecord(
                    INVITES,
                    oldInvite.getString("id"),
                    JSONObject()
                        .put("active", false)
                        .put("disabledAt", now)
                        .put("updatedAt", now)
                )
            }
        }
        updateRecord(
            WAREHOUSES,
            currentWarehouse.getString("id"),
            JSONObject().put("activeInviteHash", codeHash)
        )
        return code
    }

    suspend fun disableShareCode(warehouseId: String, ownerUid: String) {
        val warehouse = getWarehouse(warehouseId)
            ?: throw IllegalStateException("Không tìm thấy kho.")
        check(warehouse.optString("ownerUid") == ownerUid) {
            "Chỉ chủ kho mới có thể tắt mã chia sẻ."
        }
        val codeHash = warehouse.optString("activeInviteHash")
        if (codeHash.isBlank()) return
        val now = System.currentTimeMillis()
        updateRecord(
            WAREHOUSES,
            warehouse.getString("id"),
            JSONObject().put("activeInviteHash", "")
        )
        getByField(INVITES, "codeHash", codeHash)?.let { invite ->
            updateRecord(
                INVITES,
                invite.getString("id"),
                JSONObject().put("active", false).put("disabledAt", now).put("updatedAt", now)
            )
        }
    }

    suspend fun pendingShareRequests(warehouseId: String, ownerUid: String): List<String> {
        val warehouse = getWarehouse(warehouseId)
            ?: throw IllegalStateException("Không tìm thấy kho.")
        check(warehouse.optString("ownerUid") == ownerUid) {
            "Chỉ chủ kho mới xem được yêu cầu tham gia."
        }
        return listRecords(
            MEMBERS,
            "warehouseKey = ${filterValue(warehouseId)} && status = \"pending\""
        ).map { it.optString("userUid") }.filter(String::isNotBlank)
    }

    suspend fun decideShareRequest(
        warehouseId: String,
        ownerUid: String,
        memberUid: String,
        approve: Boolean
    ) {
        val warehouse = getWarehouse(warehouseId)
            ?: throw IllegalStateException("Không tìm thấy kho.")
        check(warehouse.optString("ownerUid") == ownerUid) {
            "Chỉ chủ kho mới duyệt được yêu cầu."
        }
        val member = getMember(warehouseId, memberUid)
            ?: throw IllegalStateException("Không tìm thấy yêu cầu tham gia.")
        check(member.optString("status") == "pending") {
            "Yêu cầu này không còn chờ duyệt."
        }
        updateRecord(
            MEMBERS,
            member.getString("id"),
            JSONObject()
                .put("status", if (approve) "approved" else "rejected")
                .put("decidedAt", System.currentTimeMillis())
        )
    }

    suspend fun joinWithShareCode(code: String, memberUid: String): String {
        val normalizedCode = code.filterNot(Char::isWhitespace).uppercase()
        require(normalizedCode.length == 32 && normalizedCode.all { it.isDigit() || it in 'A'..'F' }) {
            "Mã kho không đúng định dạng."
        }
        val codeHash = sha256(normalizedCode)
        val invite = getByField(INVITES, "codeHash", codeHash)
            ?: throw IllegalStateException("Mã kho không tồn tại hoặc đã được thay đổi.")
        check(invite.optBoolean("active")) {
            "Mã kho không tồn tại hoặc đã được thay đổi."
        }
        val warehouseId = invite.getString("warehouseKey")
        val warehouse = getWarehouse(warehouseId)
            ?: throw IllegalStateException("Mã kho không tồn tại hoặc đã được thay đổi.")
        check(warehouse.optString("activeInviteHash") == codeHash &&
            invite.optString("ownerUid") == warehouse.optString("ownerUid")
        ) {
            "Mã kho đã bị thay đổi. Hãy xin chủ kho cấp mã mới."
        }
        val existing = getMember(warehouseId, memberUid)
        val now = System.currentTimeMillis()
        if (existing == null) {
            createMember(
                warehouseId,
                memberUid,
                JSONObject()
                    .put("role", "member")
                    .put("status", "pending")
                    .put("inviteHash", codeHash)
                    .put("requestedAt", now)
                    .put("joinedAt", now)
            )
        } else if (existing.optString("status") in setOf("rejected", "expired")) {
            updateRecord(
                MEMBERS,
                existing.getString("id"),
                JSONObject()
                    .put("role", "member")
                    .put("status", "pending")
                    .put("inviteHash", codeHash)
                    .put("requestedAt", now)
                    .put("joinedAt", now)
            )
        } else if (existing.optString("status") == "pending") {
            updateRecord(
                MEMBERS,
                existing.getString("id"),
                JSONObject().put("inviteHash", codeHash).put("requestedAt", now)
            )
        }
        return warehouseId
    }

    fun schedule(
        context: Context,
        accountUid: String,
        storageUserId: String,
        localWarehouseId: String,
        remoteWarehouseId: String = localWarehouseId
    ) {
        if (accountUid.isBlank() || storageUserId.isBlank() || localWarehouseId.isBlank()) return
        PocketBaseAuth.initialize(context)
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
            ExistingPeriodicWorkPolicy.UPDATE,
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
            PocketBaseAuth.initialize(applicationContext)
            val session = PocketBaseAuth.session(applicationContext)
            if (session?.userId != uid) return Result.success()
            val store = LocalStore(applicationContext, storageUserId, localWarehouseId)
            return try {
                synchronize(uid, remoteWarehouseId, store)
                saveSyncError(remoteWarehouseId, null)
                Result.success()
            } catch (error: CancellationException) {
                throw error
            } catch (error: PocketBaseException) {
                saveSyncError(remoteWarehouseId, error.localizedMessage ?: "PocketBase lỗi.")
                if (error.statusCode >= 500 || error.statusCode == 408 || error.statusCode == 429) {
                    Result.retry()
                } else {
                    Result.failure(workDataOf("error" to (error.localizedMessage ?: "PocketBase lỗi")))
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

        private fun saveSyncError(warehouseId: String, error: String?) {
            val editor = applicationContext.getSharedPreferences(
                "kho-glass-preferences",
                Context.MODE_PRIVATE
            ).edit()
            if (error == null) editor.remove(syncErrorKey(warehouseId))
            else editor.putString(syncErrorKey(warehouseId), error)
            editor.apply()
        }

        private suspend fun synchronize(uid: String, warehouseId: String, store: LocalStore) {
            var warehouse = getWarehouse(warehouseId)
            if (warehouse == null) {
                try {
                    createRecord(
                        WAREHOUSES,
                        JSONObject()
                            .put("warehouseKey", warehouseId)
                            .put("ownerUid", uid)
                            .put("schemaVersion", 1)
                            .put("createdAt", System.currentTimeMillis())
                    )
                } catch (error: PocketBaseException) {
                    if (error.statusCode !in setOf(400, 409)) throw error
                }
                warehouse = getWarehouse(warehouseId)
                    ?: throw IllegalStateException("Không thể tạo kho trên PocketBase.")
            }
            val currentWarehouse = warehouse
                ?: throw IllegalStateException("Không thể tải kho từ PocketBase.")
            var member = getMember(warehouseId, uid)
            if (member == null) {
                if (currentWarehouse.optString("ownerUid") != uid) {
                    throw IllegalStateException(
                        "Tài khoản này chưa được cấp quyền truy cập kho $warehouseId."
                    )
                }
                member = createMember(
                    warehouseId,
                    uid,
                    JSONObject()
                        .put("role", "owner")
                        .put("status", "approved")
                        .put("joinedAt", System.currentTimeMillis())
                )
            }
            val currentMember = member
                ?: throw IllegalStateException("Không thể tải quyền thành viên của kho.")
            val role = currentMember.optString("role")
            val status = currentMember.optString("status")
            check(role == "owner" || status.isBlank() || status == "approved") {
                if (status == "pending") {
                    "Yêu cầu tham gia đang chờ chủ kho duyệt."
                } else {
                    "Quyền truy cập kho đã bị từ chối hoặc hết hạn. Hãy nhập lại mã và chờ chủ kho duyệt."
                }
            }
            val inviteHash = currentMember.optString("inviteHash")
            if (role != "owner" && inviteHash.isNotBlank()) {
                val invite = getByField(INVITES, "codeHash", inviteHash)
                val disabledAt = invite?.nullableLong("disabledAt")
                val lastSyncAt = currentMember.nullableLong("lastSyncAt")
                    ?: currentMember.nullableLong("joinedAt")
                val gracePeriod = TimeUnit.DAYS.toMillis(30)
                if (
                    invite?.optBoolean("active") == false &&
                    disabledAt != null &&
                    lastSyncAt != null &&
                    System.currentTimeMillis() - disabledAt >= gracePeriod &&
                    System.currentTimeMillis() - lastSyncAt >= gracePeriod
                ) {
                    updateRecord(
                        MEMBERS,
                        currentMember.getString("id"),
                        JSONObject().put("status", "expired").put("expiredAt", System.currentTimeMillis())
                    )
                    throw IllegalStateException(
                        "Mã chia sẻ đã hết hạn. Hãy nhập lại mã và chờ chủ kho duyệt."
                    )
                }
            }

            val localRecords = store.exportCloudSnapshot().toCloudRecords()
            val remoteRecords = listRecords(CHANGES, "warehouseKey = ${filterValue(warehouseId)}")
            val existingKeys = remoteRecords.mapTo(HashSet()) { it.optString("changeKey") }
            for (record in localRecords) {
                val changeKey = record.changeKey(uid)
                if (changeKey !in existingKeys) {
                    createRecord(
                        CHANGES,
                        JSONObject()
                            .put("changeKey", changeKey)
                            .put("warehouseKey", warehouseId)
                            .put("warehouse", currentWarehouse.getString("id"))
                            .put("writerUid", uid)
                            .put("entityType", record.entityType)
                            .put("entityId", record.entityId)
                            .put("payload", record.payload.toJsonObject())
                            .put("revisionAt", System.currentTimeMillis())
                    )
                    existingKeys += changeKey
                }
            }

            val cloudRecords = listRecords(CHANGES, "warehouseKey = ${filterValue(warehouseId)}")
                .mapNotNull { record ->
                    val type = record.optString("entityType").takeIf(String::isNotBlank)
                        ?: return@mapNotNull null
                    val id = record.optString("entityId").takeIf(String::isNotBlank)
                        ?: return@mapNotNull null
                    val payload = record.optJSONObject("payload")
                        ?: throw IllegalStateException("Bản ghi PocketBase ${record.optString("id")} không có payload hợp lệ.")
                    WarehouseChangeRecord(
                        type,
                        id,
                        record.serverRevisionAt(),
                        payload.toMap()
                    )
                }
            store.mergeCloudSnapshot(cloudRecords.toLatestSnapshot())
            if (role != "owner") {
                updateRecord(
                    MEMBERS,
                    currentMember.getString("id"),
                    JSONObject().put("lastSyncAt", System.currentTimeMillis())
                )
            }
        }
    }

    private suspend fun getWarehouse(key: String): JSONObject? =
        getByField(WAREHOUSES, "warehouseKey", key)

    private suspend fun getMember(warehouseKey: String, userUid: String): JSONObject? =
        getByField(MEMBERS, "membershipKey", "$warehouseKey|$userUid")

    private suspend fun createMember(
        warehouseKey: String,
        userUid: String,
        fields: JSONObject
    ): JSONObject {
        val warehouse = getWarehouse(warehouseKey)
            ?: throw IllegalStateException("Không tìm thấy kho.")
        return createRecord(
            MEMBERS,
            fields
            .put("membershipKey", "$warehouseKey|$userUid")
            .put("warehouseKey", warehouseKey)
            .put("warehouse", warehouse.getString("id"))
            .put("user", userUid)
            .put("userUid", userUid)
        )
    }

    private suspend fun getByField(
        collection: String,
        field: String,
        value: String
    ): JSONObject? = listRecords(collection, "$field = ${filterValue(value)}").firstOrNull()

    private suspend fun listRecords(
        collection: String,
        filter: String? = null
    ): List<JSONObject> {
        val records = mutableListOf<JSONObject>()
        var page = 1
        var totalPages = 1
        do {
            val query = buildString {
                append("?page=").append(page).append("&perPage=500")
                if (filter != null) {
                    append("&filter=").append(encode(filter))
                }
            }
            val response = PocketBaseAuth.request(
                "/api/collections/$collection/records$query"
            )
            val items = response.optJSONArray("items") ?: JSONArray()
            for (index in 0 until items.length()) {
                items.optJSONObject(index)?.let(records::add)
            }
            totalPages = response.optInt("totalPages", page)
            page++
        } while (page <= totalPages)
        return records
    }

    private suspend fun createRecord(collection: String, fields: JSONObject): JSONObject =
        PocketBaseAuth.request(
            "/api/collections/$collection/records",
            "POST",
            fields
        )

    private suspend fun updateRecord(
        collection: String,
        id: String,
        fields: JSONObject
    ): JSONObject = PocketBaseAuth.request(
        "/api/collections/$collection/records/$id",
        "PATCH",
        fields
    )

    private fun filterValue(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}

internal data class WarehouseChangeRecord(
    val entityType: String,
    val entityId: String,
    val revisionAt: Long,
    val payload: Map<String, Any?>
) {
    fun changeKey(uid: String): String =
        sha256("$uid|$entityType|$entityId|${canonical(payload)}")
}

private fun CloudWarehouseSnapshot.toCloudRecords(): List<WarehouseChangeRecord> = buildList {
    products.forEach { row ->
        add(WarehouseChangeRecord("products", row.id, 0L, row.toCloudMap().toMutableMap().apply {
            put("imagePath", null)
            put("additionalImagePaths", "[]")
        }))
    }
    branches.forEach { add(WarehouseChangeRecord("branches", it.id, 0L, it.toCloudMap())) }
    batches.forEach { add(WarehouseChangeRecord("batches", it.id, 0L, it.toCloudMap())) }
    movements.forEach { add(WarehouseChangeRecord("movements", it.id, 0L, it.toCloudMap())) }
    orders.forEach { add(WarehouseChangeRecord("orders", it.id, 0L, it.toCloudMap())) }
    orderLines.forEach { add(WarehouseChangeRecord("orderLines", it.id, 0L, it.toCloudMap())) }
    allocations.forEach { add(WarehouseChangeRecord("allocations", it.id, 0L, it.toCloudMap())) }
    payments.forEach { add(WarehouseChangeRecord("payments", it.id, 0L, it.toCloudMap())) }
}

private fun Any.toCloudMap(): Map<String, Any?> =
    javaClass.declaredFields
        .filterNot { field -> field.isSynthetic || java.lang.reflect.Modifier.isStatic(field.modifiers) }
        .associate { field ->
            field.isAccessible = true
            field.name to field.get(this)
        }

private fun List<WarehouseChangeRecord>.toLatestSnapshot(): CloudWarehouseSnapshot {
    val allRecords = this
    val latest = groupBy { it.entityType to it.entityId }
        .mapValues { (_, revisions) ->
            revisions.maxWithOrNull(compareBy<WarehouseChangeRecord> { it.revisionAt }.thenBy {
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
            archivedAt = data.nullableLong("archivedAt"),
            deletedAt = data.nullableLong("deletedAt"),
            unitSummaryLabel = data.string("unitSummaryLabel"),
            additionalImagePaths = "[]",
            preDeleteActive = data["preDeleteActive"] as? Boolean ?: false
        )
        if (product.id in permanentlyDeletedProductIds) {
            product.copy(active = false, permanentlyDeleted = true)
        } else product
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
            lowStockThreshold = data.double("lowStockThreshold"),
            archivedStock = (data["archivedStock"] as? Number)?.toDouble() ?: 0.0
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
            payments.filter { it.string("kind") == "PAYMENT" }.sumOf { it.long("amount") }
        }
    val refundTotals = rows("payments")
        .groupBy { it.string("orderId") }
        .mapValues { (_, payments) ->
            payments.filter { it.string("kind") == "REFUND" }.sumOf { it.long("amount") }
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
                } else null
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

private fun JSONObject.toMap(): Map<String, Any?> = buildMap {
    val keys = keys()
    while (keys.hasNext()) {
        val key = keys.next()
        val value = get(key)
        put(
            key,
            when (value) {
                JSONObject.NULL -> null
                is JSONObject -> value.toMap()
                is JSONArray -> (0 until value.length()).map { index ->
                    value.get(index).let { item ->
                        if (item is JSONObject) item.toMap() else item
                    }
                }
                else -> value
            }
        )
    }
}

private fun Map<String, Any?>.toJsonObject(): JSONObject = JSONObject().also { json ->
    forEach { (key, value) ->
        json.put(key, value ?: JSONObject.NULL)
    }
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

private fun JSONObject.nullableLong(key: String): Long? =
    if (isNull(key)) null else optLong(key).takeIf { has(key) }

private fun JSONObject.serverRevisionAt(): Long {
    val created = optString("created")
    if (created.isNotBlank()) {
        try {
            return Instant.parse(created.replace(' ', 'T')).toEpochMilli()
        } catch (_: DateTimeParseException) {
            // Fall back to the explicit revision field for older server formats.
        }
    }
    return nullableLong("revisionAt") ?: 0L
}

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
