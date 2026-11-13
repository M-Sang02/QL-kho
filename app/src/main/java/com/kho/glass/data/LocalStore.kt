package com.kho.glass.data

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import androidx.room.withTransaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import java.io.IOException
import java.io.File
import java.util.UUID

@Entity(tableName = "products")
data class ProductRecord(
    @PrimaryKey val id: String,
    val name: String,
    val active: Boolean = true,
    val createdAt: Long,
    val imagePath: String? = null,
    val permanentlyDeleted: Boolean = false,
    val unitSummaryLabel: String = "loại",
    @ColumnInfo(defaultValue = "'[]'")
    val additionalImagePaths: String = "[]"
)

@Entity(
    tableName = "unit_branches",
    foreignKeys = [ForeignKey(
        entity = ProductRecord::class,
        parentColumns = ["id"],
        childColumns = ["productId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("productId")]
)
data class UnitBranchRecord(
    @PrimaryKey val id: String,
    val productId: String,
    val name: String,
    val unitType: String,
    val stock: Double,
    val buyPrice: Long,
    val sellPrice: Long,
    val lowStockThreshold: Double
)

@Entity(
    tableName = "stock_batches",
    foreignKeys = [ForeignKey(
        entity = UnitBranchRecord::class,
        parentColumns = ["id"],
        childColumns = ["branchId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("branchId"), Index("receivedAt")]
)
data class StockBatchRecord(
    @PrimaryKey val id: String,
    val branchId: String,
    val receivedAt: Long,
    val quantityReceived: Double,
    val quantityRemaining: Double,
    val unitCost: Long,
    val approvalStatus: String
)

@Entity(
    tableName = "stock_movements",
    foreignKeys = [ForeignKey(
        entity = UnitBranchRecord::class,
        parentColumns = ["id"],
        childColumns = ["branchId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("branchId"), Index("happenedAt"), Index("referenceId")]
)
data class StockMovementRecord(
    @PrimaryKey val id: String,
    val branchId: String,
    val type: String,
    val quantityDelta: Double,
    val happenedAt: Long,
    val referenceId: String?,
    val syncStatus: String
)

@Entity(tableName = "sales_orders", indices = [Index("createdAt"), Index("status")])
data class SalesOrderRecord(
    @PrimaryKey val id: String,
    val customerName: String,
    val createdAt: Long,
    val completedAt: Long?,
    val status: String,
    val totalAmount: Long,
    val paidAmount: Long,
    val refundPending: Long = 0L
)

@Entity(
    tableName = "sales_order_lines",
    foreignKeys = [ForeignKey(
        entity = SalesOrderRecord::class,
        parentColumns = ["id"],
        childColumns = ["orderId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("orderId"), Index("branchId"), Index(value = ["orderId", "position"])]
)
data class SalesOrderLineRecord(
    @PrimaryKey val id: String,
    val orderId: String,
    val branchId: String,
    val productName: String,
    val branchName: String,
    val quantity: Double,
    val unitPrice: Long,
    val lineTotal: Long,
    val position: Int,
    val productDeleted: Boolean = false
)

@Entity(
    tableName = "order_line_batch_allocations",
    foreignKeys = [
        ForeignKey(
            entity = SalesOrderLineRecord::class,
            parentColumns = ["id"],
            childColumns = ["orderLineId"],
            onDelete = ForeignKey.RESTRICT
        ),
        ForeignKey(
            entity = StockBatchRecord::class,
            parentColumns = ["id"],
            childColumns = ["batchId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("orderLineId"), Index("batchId")]
)
data class OrderLineBatchAllocationRecord(
    @PrimaryKey val id: String,
    val orderLineId: String,
    val batchId: String,
    val quantity: Double,
    val unitCost: Long
)

@Entity(
    tableName = "order_payments",
    foreignKeys = [ForeignKey(
        entity = SalesOrderRecord::class,
        parentColumns = ["id"],
        childColumns = ["orderId"],
        onDelete = ForeignKey.RESTRICT
    )],
    indices = [Index("orderId"), Index("happenedAt")]
)
data class OrderPaymentRecord(
    @PrimaryKey val id: String,
    val orderId: String,
    val amount: Long,
    val happenedAt: Long,
    val kind: String
)

data class ProductWithBranches(
    @Embedded val product: ProductRecord,
    @Relation(parentColumn = "id", entityColumn = "productId")
    val branches: List<UnitBranchRecord>
)

@Dao
interface KhoDao {
    @Query("SELECT * FROM products")
    suspend fun getAllProducts(): List<ProductRecord>

    @Query("SELECT * FROM unit_branches")
    suspend fun getAllBranches(): List<UnitBranchRecord>

    @Query("SELECT * FROM stock_batches")
    suspend fun getAllBatches(): List<StockBatchRecord>

    @Query("SELECT * FROM stock_movements")
    suspend fun getAllMovements(): List<StockMovementRecord>

    @Query("SELECT * FROM sales_orders")
    suspend fun getAllOrders(): List<SalesOrderRecord>

    @Query("SELECT * FROM sales_order_lines")
    suspend fun getAllOrderLines(): List<SalesOrderLineRecord>

    @Query("SELECT * FROM order_line_batch_allocations")
    suspend fun getAllAllocations(): List<OrderLineBatchAllocationRecord>

    @Query("SELECT * FROM order_payments")
    suspend fun getAllPayments(): List<OrderPaymentRecord>

    @Upsert
    suspend fun upsertProducts(rows: List<ProductRecord>)

    @Upsert
    suspend fun upsertBranches(rows: List<UnitBranchRecord>)

    @Upsert
    suspend fun upsertBatches(rows: List<StockBatchRecord>)

    @Upsert
    suspend fun upsertMovements(rows: List<StockMovementRecord>)

    @Upsert
    suspend fun upsertOrders(rows: List<SalesOrderRecord>)

    @Upsert
    suspend fun upsertOrderLines(rows: List<SalesOrderLineRecord>)

    @Upsert
    suspend fun upsertAllocations(rows: List<OrderLineBatchAllocationRecord>)

    @Upsert
    suspend fun upsertPayments(rows: List<OrderPaymentRecord>)

    @Transaction
    @Query("SELECT * FROM products WHERE active = 1 AND permanentlyDeleted = 0 ORDER BY name COLLATE NOCASE")
    fun observeProducts(): Flow<List<ProductWithBranches>>

    @Transaction
    @Query("SELECT * FROM products WHERE active = 0 AND permanentlyDeleted = 0 ORDER BY name COLLATE NOCASE")
    fun observeArchivedProducts(): Flow<List<ProductWithBranches>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProduct(id: String): ProductRecord?

    @Query("SELECT * FROM products WHERE active = 1 AND permanentlyDeleted = 0")
    suspend fun getActiveProducts(): List<ProductRecord>

    @Query("SELECT * FROM unit_branches WHERE id = :id LIMIT 1")
    suspend fun getBranch(id: String): UnitBranchRecord?

    @Query("SELECT * FROM unit_branches WHERE productId = :productId")
    suspend fun getProductBranches(productId: String): List<UnitBranchRecord>

    @Query("SELECT * FROM products WHERE permanentlyDeleted = 0")
    suspend fun getProductsForPermanentDeletion(): List<ProductRecord>

    @Query("SELECT imagePath FROM products WHERE imagePath IS NOT NULL")
    suspend fun getProductImagePaths(): List<String>

    @Query("SELECT additionalImagePaths FROM products")
    suspend fun getAdditionalProductImagePaths(): List<String>

    @Query("SELECT * FROM unit_branches WHERE productId = :productId AND name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findBranchByName(productId: String, name: String): UnitBranchRecord?

    @Query("SELECT * FROM unit_branches WHERE id = :id LIMIT 1")
    fun observeBranch(id: String): Flow<UnitBranchRecord?>

    @Query("SELECT * FROM stock_batches WHERE branchId = :branchId AND quantityRemaining > 0 AND approvalStatus = 'APPROVED' ORDER BY receivedAt, id")
    suspend fun getAvailableBatches(branchId: String): List<StockBatchRecord>

    @Query("SELECT * FROM sales_orders ORDER BY createdAt DESC")
    fun observeOrders(): Flow<List<SalesOrderRecord>>

    @Query(
        "SELECT m.id, m.branchId, m.type, m.quantityDelta, m.happenedAt, m.referenceId, " +
            "m.syncStatus, p.name AS productName, b.name AS branchName " +
            "FROM stock_movements m " +
            "INNER JOIN unit_branches b ON b.id = m.branchId " +
            "INNER JOIN products p ON p.id = b.productId " +
            "ORDER BY m.happenedAt DESC"
    )
    fun observeStockMovements(): Flow<List<StockMovementView>>

    @Query("SELECT * FROM sales_order_lines WHERE orderId = :orderId ORDER BY position")
    fun observeOrderLines(orderId: String): Flow<List<SalesOrderLineRecord>>

    @Query("SELECT * FROM sales_order_lines WHERE orderId = :orderId")
    suspend fun getOrderLines(orderId: String): List<SalesOrderLineRecord>

    @Query("UPDATE sales_order_lines SET productDeleted = 1 WHERE branchId IN (:branchIds)")
    suspend fun markOrderLinesProductDeleted(branchIds: List<String>)

    @Query(
        "UPDATE sales_order_lines SET productDeleted = 1 WHERE branchId IN " +
            "(SELECT id FROM unit_branches WHERE productId IN " +
            "(SELECT id FROM products WHERE permanentlyDeleted = 0))"
    )
    suspend fun markAllOrderLinesProductsDeleted()

    @Query("SELECT * FROM order_line_batch_allocations WHERE orderLineId = :lineId")
    suspend fun getAllocations(lineId: String): List<OrderLineBatchAllocationRecord>

    @Query("SELECT * FROM order_payments WHERE orderId = :orderId ORDER BY happenedAt")
    fun observePayments(orderId: String): Flow<List<OrderPaymentRecord>>

    @Insert
    suspend fun insertProduct(product: ProductRecord)

    @Query(
        "UPDATE products SET name = :name, imagePath = :imagePath, " +
            "additionalImagePaths = :additionalImagePaths WHERE id = :productId"
    )
    suspend fun updateProduct(
        productId: String,
        name: String,
        imagePath: String?,
        additionalImagePaths: String
    )

    @Query("UPDATE products SET unitSummaryLabel = :label WHERE id = :productId")
    suspend fun updateUnitSummaryLabel(productId: String, label: String)

    @Query("UPDATE products SET active = 0 WHERE id = :productId")
    suspend fun deactivateProduct(productId: String)

    @Query("UPDATE products SET active = 1 WHERE id = :productId")
    suspend fun reactivateProduct(productId: String)

    @Query(
        "UPDATE products SET active = 0, permanentlyDeleted = 1, imagePath = NULL, " +
            "additionalImagePaths = '[]' WHERE id = :productId"
    )
    suspend fun permanentlyDeleteProduct(productId: String)

    @Query(
        "UPDATE products SET active = 0, permanentlyDeleted = 1, imagePath = NULL, " +
            "additionalImagePaths = '[]' WHERE permanentlyDeleted = 0"
    )
    suspend fun permanentlyDeleteAllProducts()

    @Insert
    suspend fun insertBranch(branch: UnitBranchRecord)

    @Insert
    suspend fun insertBatch(batch: StockBatchRecord)

    @Insert
    suspend fun insertMovement(movement: StockMovementRecord)

    @Insert
    suspend fun insertOrder(order: SalesOrderRecord)

    @Insert
    suspend fun insertOrderLine(line: SalesOrderLineRecord)

    @Insert
    suspend fun insertAllocation(allocation: OrderLineBatchAllocationRecord)

    @Insert
    suspend fun insertPayment(payment: OrderPaymentRecord)

    @Query("UPDATE unit_branches SET stock = stock + :delta WHERE id = :branchId")
    suspend fun adjustStock(branchId: String, delta: Double)

    @Query("UPDATE unit_branches SET buyPrice = :price WHERE id = :branchId")
    suspend fun updateBuyPrice(branchId: String, price: Long)

    @Query("UPDATE unit_branches SET sellPrice = :price WHERE id = :branchId")
    suspend fun updateSellPrice(branchId: String, price: Long)

    @Query("UPDATE unit_branches SET stock = :stock, buyPrice = :buyPrice, sellPrice = :sellPrice WHERE id = :branchId")
    suspend fun updateBranchInventory(branchId: String, stock: Double, buyPrice: Long, sellPrice: Long)

    @Query("UPDATE stock_batches SET quantityRemaining = quantityRemaining - :quantity WHERE id = :batchId")
    suspend fun consumeBatch(batchId: String, quantity: Double)

    @Query("UPDATE stock_batches SET quantityRemaining = quantityRemaining + :quantity WHERE id = :batchId")
    suspend fun restoreBatch(batchId: String, quantity: Double)

    @Query("UPDATE sales_orders SET paidAmount = :paidAmount, status = :status, completedAt = :completedAt WHERE id = :orderId")
    suspend fun updateOrderPayment(orderId: String, paidAmount: Long, status: String, completedAt: Long?)

    @Query("SELECT * FROM sales_orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrder(orderId: String): SalesOrderRecord?

    @Query("UPDATE sales_orders SET status = 'CANCELLED', refundPending = :refundAmount WHERE id = :orderId")
    suspend fun cancelOrder(orderId: String, refundAmount: Long)

    @Query("UPDATE sales_orders SET refundPending = 0 WHERE id = :orderId")
    suspend fun clearPendingRefund(orderId: String)
}

@Database(
    entities = [
        ProductRecord::class,
        UnitBranchRecord::class,
        StockBatchRecord::class,
        StockMovementRecord::class,
        SalesOrderRecord::class,
        SalesOrderLineRecord::class,
        OrderLineBatchAllocationRecord::class,
        OrderPaymentRecord::class
    ],
    version = 5,
    exportSchema = false
)
abstract class KhoDatabase : RoomDatabase() {
    abstract fun dao(): KhoDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN imagePath TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE products ADD COLUMN permanentlyDeleted INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE sales_order_lines ADD COLUMN productDeleted INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE products ADD COLUMN unitSummaryLabel TEXT NOT NULL DEFAULT 'loại'"
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE products ADD COLUMN additionalImagePaths TEXT NOT NULL DEFAULT '[]'"
                )
            }
        }
    }
}

class LocalStore(
    context: Context,
    userId: String,
    private val warehouseId: String = "default"
) {
    init {
        require(userId.isNotBlank()) { "A signed-in user is required for local storage." }
    }

    private val database = Room.databaseBuilder(
        context.applicationContext,
        KhoDatabase::class.java,
        databaseName(userId, warehouseId)
    ).addMigrations(
        KhoDatabase.MIGRATION_1_2,
        KhoDatabase.MIGRATION_2_3,
        KhoDatabase.MIGRATION_3_4,
        KhoDatabase.MIGRATION_4_5
    ).build()

    val products: Flow<List<Product>> =
        database.dao().observeProducts().map { rows -> rows.map { it.toModel() } }

    val archivedProducts: Flow<List<Product>> =
        database.dao().observeArchivedProducts().map { rows -> rows.map { it.toModel() } }

    val orders: Flow<List<SalesOrder>> = database.dao().observeOrders().let { rows ->
        rows.map { records ->
            records.map { it.toModel() }
        }
    }

    val stockMovements: Flow<List<StockMovementView>> = database.dao().observeStockMovements()

    suspend fun exportProducts(): List<Product> =
        database.dao().observeProducts().first().map { it.toModel() }

    suspend fun exportCloudSnapshot(): CloudWarehouseSnapshot = database.withTransaction {
        val dao = database.dao()
        CloudWarehouseSnapshot(
            products = dao.getAllProducts(),
            branches = dao.getAllBranches(),
            batches = dao.getAllBatches(),
            movements = dao.getAllMovements(),
            orders = dao.getAllOrders(),
            orderLines = dao.getAllOrderLines(),
            allocations = dao.getAllAllocations(),
            payments = dao.getAllPayments()
        )
    }

    suspend fun mergeCloudSnapshot(snapshot: CloudWarehouseSnapshot) {
        database.withTransaction {
            val dao = database.dao()
            val existingProducts = dao.getAllProducts().associateBy { it.id }
            val mergedProducts = snapshot.products.map { product ->
                product.copy(
                    imagePath = if (product.permanentlyDeleted) null
                    else existingProducts[product.id]?.imagePath,
                    additionalImagePaths = if (product.permanentlyDeleted) "[]"
                    else existingProducts[product.id]?.additionalImagePaths ?: "[]"
                )
            }
            val movementTotals = snapshot.movements.groupBy { it.branchId }
                .mapValues { (_, movements) -> movements.sumOf { it.quantityDelta } }
            val mergedBranches = snapshot.branches.map { branch ->
                branch.copy(stock = movementTotals[branch.id] ?: branch.stock)
            }
            val cancelledOrderIds = snapshot.orders
                .filter { it.status == "CANCELLED" }
                .mapTo(HashSet()) { it.id }
            val lineIdsForActiveOrders = snapshot.orderLines
                .filterNot { it.orderId in cancelledOrderIds }
                .mapTo(HashSet()) { it.id }
            val allocatedByBatch = snapshot.allocations
                .filter { it.orderLineId in lineIdsForActiveOrders }
                .groupBy { it.batchId }
                .mapValues { (_, allocations) -> allocations.sumOf { it.quantity } }
            val mergedBatches = snapshot.batches.map { batch ->
                batch.copy(
                    quantityRemaining = batch.quantityReceived -
                        (allocatedByBatch[batch.id] ?: 0.0).coerceAtMost(batch.quantityReceived)
                )
            }
            val paymentsByOrder = snapshot.payments
                .filter { it.kind == "PAYMENT" }
                .groupBy { it.orderId }
                .mapValues { (_, payments) -> payments.sumOf { it.amount } }
            val mergedOrders = snapshot.orders.map { order ->
                if (order.status == "CANCELLED") {
                    order
                } else {
                    val paidAmount = paymentsByOrder[order.id] ?: order.paidAmount
                    order.copy(
                        paidAmount = paidAmount,
                        status = when {
                            order.totalAmount > 0L && paidAmount >= order.totalAmount -> "COMPLETED"
                            paidAmount > 0L -> "PARTIALLY_PAID"
                            else -> "UNPAID"
                        }
                    )
                }
            }
            dao.upsertProducts(mergedProducts)
            dao.upsertBranches(mergedBranches)
            dao.upsertBatches(mergedBatches)
            dao.upsertMovements(snapshot.movements)
            dao.upsertOrders(mergedOrders)
            dao.upsertOrderLines(snapshot.orderLines)
            dao.upsertAllocations(snapshot.allocations)
            dao.upsertPayments(snapshot.payments)
        }
    }

    fun close() {
        database.close()
    }

    data class CloudWarehouseSnapshot(
        val products: List<ProductRecord>,
        val branches: List<UnitBranchRecord>,
        val batches: List<StockBatchRecord>,
        val movements: List<StockMovementRecord>,
        val orders: List<SalesOrderRecord>,
        val orderLines: List<SalesOrderLineRecord>,
        val allocations: List<OrderLineBatchAllocationRecord>,
        val payments: List<OrderPaymentRecord>
    )

    companion object {
        private fun databaseName(userId: String, warehouseId: String): String {
            val userKey = UUID.nameUUIDFromBytes(userId.toByteArray())
            return if (warehouseId == "default") {
                "kho-glass-$userKey.db"
            } else {
                "kho-glass-$userKey-${UUID.nameUUIDFromBytes(warehouseId.toByteArray())}.db"
            }
        }

        suspend fun deleteWarehouseDatabase(
            context: Context,
            userId: String,
            warehouseId: String
        ): List<String> {
            require(userId.isNotBlank()) { "A signed-in user is required for local storage." }
            val appContext = context.applicationContext
            val store = LocalStore(appContext, userId, warehouseId)
            val imagePaths = try {
                store.database.dao().getProductImagePaths() +
                    store.database.dao().getAdditionalProductImagePaths()
                        .flatMap { it.decodeImagePaths() }
            } finally {
                store.close()
            }
            check(appContext.deleteDatabase(databaseName(userId, warehouseId))) {
                "Không xóa được dữ liệu kho."
            }
            val imageDirectory = File(appContext.filesDir, "product-images").canonicalFile
            return imagePaths.mapNotNull { path ->
                try {
                    val image = File(path).canonicalFile
                    if (image.parentFile == imageDirectory && image.exists() && !image.delete()) {
                        image.name
                    } else {
                        null
                    }
                } catch (_: IOException) {
                    File(path).name
                }
            }
        }
    }

    fun observeOrderLines(orderId: String): Flow<List<SalesOrderLine>> =
        database.dao().observeOrderLines(orderId).let { rows ->
            rows.map { records ->
                records.map {
                    SalesOrderLine(
                        id = it.id,
                        orderId = it.orderId,
                        branchId = it.branchId,
                        productName = it.productName,
                        branchName = it.branchName,
                        quantity = it.quantity,
                        unitPrice = it.unitPrice,
                        lineTotal = it.lineTotal,
                        productDeleted = it.productDeleted
                    )
                }
            }
        }

    fun observePayments(orderId: String): Flow<List<OrderPayment>> =
        database.dao().observePayments(orderId).let { rows ->
            rows.map { records ->
                records.map {
                    OrderPayment(it.id, it.orderId, it.amount, it.happenedAt, it.kind)
                }
            }
        }

    suspend fun addProduct(
        name: String,
        unitType: UnitType,
        openingStock: Double,
        buyPrice: Long,
        sellPrice: Long,
        imagePath: String?,
        additionalImagePaths: List<String> = emptyList()
    ) {
        val normalizedName = name.trim().replace(Regex("\\s+"), " ").replaceFirstChar { it.uppercase() }
        require(normalizedName.isNotBlank()) { "Nhập tên sản phẩm." }
        require(openingStock.isFinite() && openingStock >= 0.0) { "Tồn đầu kỳ không hợp lệ." }
        require(buyPrice >= 0L) { "Giá nhập không hợp lệ." }
        require(sellPrice >= 0L) { "Giá bán không hợp lệ." }
        require(unitType != UnitType.OTHER) { "Chọn đơn vị Cái hoặc Kg." }
        if (unitType == UnitType.PIECE) {
            require(openingStock % 1.0 == 0.0) { "Số lượng Cái phải là số nguyên." }
        }

        val productId = UUID.randomUUID().toString()
        val branchId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        database.withTransaction {
            val dao = database.dao()
            check(dao.getActiveProducts().none {
                normalizeProductName(it.name).equals(normalizedName, ignoreCase = true)
            }) {
                "Sản phẩm này đã có trong danh sách. Hãy mở sản phẩm hiện có để nhập thêm hàng."
            }
            dao.insertProduct(
                ProductRecord(
                    productId,
                    normalizedName,
                    createdAt = now,
                    imagePath = imagePath,
                    additionalImagePaths = additionalImagePaths.encodeImagePaths()
                )
            )
            dao.insertBranch(
                UnitBranchRecord(
                    id = branchId,
                    productId = productId,
                    name = when (unitType) {
                        UnitType.PIECE -> "Cái"
                        UnitType.WEIGHT -> "Kg"
                        UnitType.OTHER -> throw IllegalArgumentException("Chọn đơn vị Cái hoặc Kg.")
                    },
                    unitType = unitType.name,
                    stock = openingStock,
                    buyPrice = buyPrice,
                    sellPrice = sellPrice,
                    lowStockThreshold = if (unitType == UnitType.WEIGHT) 10.0 else 2.0
                )
            )
            if (openingStock > 0.0) {
                val batchId = UUID.randomUUID().toString()
                dao.insertBatch(
                    StockBatchRecord(
                        id = batchId,
                        branchId = branchId,
                        receivedAt = now,
                        quantityReceived = openingStock,
                        quantityRemaining = openingStock,
                        unitCost = buyPrice,
                        approvalStatus = "APPROVED"
                    )
                )
                dao.insertMovement(
                    StockMovementRecord(
                        id = UUID.randomUUID().toString(),
                        branchId = branchId,
                        type = "OPENING",
                        quantityDelta = openingStock,
                        happenedAt = now,
                        referenceId = batchId,
                        syncStatus = "PENDING"
                    )
                )
            }
        }
    }

    suspend fun updateProduct(
        productId: String,
        name: String,
        imagePath: String?,
        additionalImagePaths: List<String>? = null
    ) {
        val normalizedName = name.trim().replace(Regex("\\s+"), " ").replaceFirstChar { it.uppercase() }
        require(normalizedName.isNotBlank()) { "Nhập tên sản phẩm." }
        val removedImagePaths = database.withTransaction {
            val dao = database.dao()
            val product = checkNotNull(dao.getProduct(productId)) { "Không tìm thấy sản phẩm." }
            val duplicate = dao.getActiveProducts().any {
                it.id != productId &&
                    normalizeProductName(it.name).equals(normalizedName, ignoreCase = true)
            }

            require(!duplicate) {
                "Tên này đang được dùng cho sản phẩm khác."
            }
            val oldImages = listOfNotNull(product.imagePath) + product.additionalImagePaths.decodeImagePaths()
            val retainedAdditionalImages =
                additionalImagePaths ?: product.additionalImagePaths.decodeImagePaths()
            val retainedImages = listOfNotNull(imagePath) + retainedAdditionalImages
            dao.updateProduct(
                productId,
                normalizedName,
                imagePath,
                retainedAdditionalImages.encodeImagePaths()
            )
            oldImages.distinct() - retainedImages.toSet()
        }
        deleteProductImages(removedImagePaths)
    }

    suspend fun updateUnitSummaryLabel(productId: String, label: String) {
        database.withTransaction {
            checkNotNull(database.dao().getProduct(productId)) { "Không tìm thấy sản phẩm." }
            database.dao().updateUnitSummaryLabel(
                productId,
                label.trim().replace(Regex("\\s+"), " ")
            )
        }
    }

    suspend fun deactivateProduct(productId: String) {
        database.withTransaction {
            val dao = database.dao()
            checkNotNull(dao.getProduct(productId)) { "Không tìm thấy sản phẩm." }
            dao.deactivateProduct(productId)
        }
    }

    suspend fun reactivateProduct(productId: String) {
        database.withTransaction {
            val dao = database.dao()
            val product = checkNotNull(dao.getProduct(productId)) { "Không tìm thấy sản phẩm." }
            val duplicate = dao.getActiveProducts().any {
                normalizeProductName(it.name).equals(
                    normalizeProductName(product.name),
                    ignoreCase = true
                )
            }
            require(!duplicate) { "Đã có sản phẩm đang dùng tên này." }
            dao.reactivateProduct(productId)
        }
    }

    suspend fun permanentlyDeleteProduct(productId: String) {
        var imagePaths: List<String> = emptyList()
        database.withTransaction {
            val dao = database.dao()
            val product = checkNotNull(dao.getProduct(productId)) { "Không tìm thấy sản phẩm." }
            val branchIds = dao.getProductBranches(productId).map { it.id }
            dao.markOrderLinesProductDeleted(branchIds)
            imagePaths = listOfNotNull(product.imagePath) + product.additionalImagePaths.decodeImagePaths()
            dao.permanentlyDeleteProduct(productId)
        }
        deleteProductImages(imagePaths)
    }

    suspend fun permanentlyDeleteAllProducts() {
        val imagePaths = mutableListOf<String>()
        database.withTransaction {
            val dao = database.dao()
            val products = dao.getProductsForPermanentDeletion()
            if (products.isNotEmpty()) dao.markAllOrderLinesProductsDeleted()
            imagePaths += products.flatMap {
                listOfNotNull(it.imagePath) + it.additionalImagePaths.decodeImagePaths()
            }
            dao.permanentlyDeleteAllProducts()
        }
        deleteProductImages(imagePaths.distinct())
    }

    suspend fun importInventory(decisions: List<InventoryImportDecision>) {
        require(decisions.isNotEmpty()) { "Tệp sao lưu không có sản phẩm để nhập." }
        val now = System.currentTimeMillis()
        val replacedImagePaths = mutableListOf<String>()
        try {
            database.withTransaction {
                val dao = database.dao()
                for (decision in decisions) {
                    if (decision.action == DuplicateImportAction.SKIP) continue
                    val item = decision.product
                    val normalizedName = normalizeProductName(
                        if (decision.action == DuplicateImportAction.CREATE_NEW) {
                            decision.newName ?: throw IllegalArgumentException("Nhập tên cho sản phẩm mới.")
                        } else {
                            item.name
                        }
                    ).replaceFirstChar { it.uppercase() }
                    require(normalizedName.isNotBlank()) { "Tên sản phẩm không hợp lệ." }
                    val duplicate = dao.getActiveProducts().firstOrNull {
                        normalizeProductName(it.name).equals(normalizedName, ignoreCase = true)
                    }
                    if (decision.action == DuplicateImportAction.CREATE_NEW) {
                        require(duplicate == null) { "Tên “$normalizedName” đã được sử dụng." }
                    }
                    val existing = if (decision.action == DuplicateImportAction.KEEP_CURRENT_PRICES) {
                        checkNotNull(duplicate) { "Không tìm thấy sản phẩm trùng “$normalizedName”." }
                    } else {
                        null
                    }
                    val product = existing ?: ProductRecord(
                        id = UUID.randomUUID().toString(),
                        name = normalizedName,
                        createdAt = now,
                        imagePath = item.imagePath,
                        additionalImagePaths = item.additionalImagePaths.encodeImagePaths()
                    ).also { dao.insertProduct(it) }
                    if (
                        existing != null &&
                        (item.imagePath != null || item.additionalImagePaths.isNotEmpty())
                    ) {
                        val nextImagePath = item.imagePath ?: product.imagePath
                        val nextAdditionalImages = if (item.additionalImagePaths.isNotEmpty()) {
                            item.additionalImagePaths
                        } else {
                            product.additionalImagePaths.decodeImagePaths()
                        }
                        val retainedImages = listOfNotNull(nextImagePath) + nextAdditionalImages
                        replacedImagePaths += (
                            listOfNotNull(product.imagePath) +
                                product.additionalImagePaths.decodeImagePaths()
                            ).filterNot { it in retainedImages }
                        dao.updateProduct(
                            product.id,
                            normalizedName,
                            nextImagePath,
                            nextAdditionalImages.encodeImagePaths()
                        )
                    }
                    for (unit in item.units) {
                        require(unit.name.isNotBlank()) { "Tên đơn vị của “$normalizedName” không hợp lệ." }
                        require(unit.quantity.isFinite() && unit.quantity >= 0.0) {
                            "Tồn kho không hợp lệ cho “$normalizedName”."
                        }
                        require(unit.unitType != UnitType.PIECE || unit.quantity % 1.0 == 0.0) {
                            "Số lượng Cái không hợp lệ cho “$normalizedName”."
                        }
                        require(unit.buyPrice >= 0L && unit.sellPrice >= 0L) {
                            "Giá không hợp lệ cho “$normalizedName”."
                        }
                        val existingBranch = dao.findBranchByName(product.id, unit.name)
                        if (existingBranch == null) {
                            val branchId = UUID.randomUUID().toString()
                            dao.insertBranch(
                                UnitBranchRecord(
                                    id = branchId,
                                    productId = product.id,
                                    name = unit.name,
                                    unitType = unit.unitType.name,
                                    stock = unit.quantity,
                                    buyPrice = unit.buyPrice,
                                    sellPrice = unit.sellPrice,
                                    lowStockThreshold = if (unit.unitType == UnitType.WEIGHT) 10.0 else 2.0
                                )
                            )
                            if (unit.quantity > 0.0) {
                                insertInventoryBatch(
                                    dao,
                                    branchId,
                                    unit.quantity,
                                    unit.buyPrice,
                                    now,
                                    "RESTORE"
                                )
                            }
                        } else {
                            require(existingBranch.unitType == unit.unitType.name) {
                                "Đơn vị “${unit.name}” của “$normalizedName” không khớp."
                            }
                            val preservePrices = decision.action == DuplicateImportAction.KEEP_CURRENT_PRICES
                            val effectiveBuyPrice = if (preservePrices) {
                                existingBranch.buyPrice
                            } else {
                                unit.buyPrice
                            }
                            val effectiveSellPrice = if (preservePrices) {
                                existingBranch.sellPrice
                            } else {
                                unit.sellPrice
                            }
                            val delta = unit.quantity - existingBranch.stock
                            if (delta > 0.0) {
                                insertInventoryBatch(
                                    dao,
                                    existingBranch.id,
                                    delta,
                                    effectiveBuyPrice,
                                    now,
                                    "RESTORE"
                                )
                            } else if (delta < 0.0) {
                                var toRemove = -delta
                                for (batch in dao.getAvailableBatches(existingBranch.id).asReversed()) {
                                    if (toRemove <= 0.0) break
                                    val removed = minOf(toRemove, batch.quantityRemaining)
                                    dao.consumeBatch(batch.id, removed)
                                    toRemove -= removed
                                }
                                check(toRemove <= 1e-8) {
                                    "Không thể khôi phục tồn cho “$normalizedName”: số dư lô không khớp."
                                }
                            }
                            dao.updateBranchInventory(
                                existingBranch.id,
                                unit.quantity,
                                effectiveBuyPrice,
                                effectiveSellPrice
                            )
                            if (delta != 0.0) {
                                dao.insertMovement(
                                    StockMovementRecord(
                                        id = UUID.randomUUID().toString(),
                                        branchId = existingBranch.id,
                                        type = "RESTORE",
                                        quantityDelta = delta,
                                        happenedAt = now,
                                        referenceId = null,
                                        syncStatus = "PENDING"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (failure: Exception) {
            try {
                InventoryBackup.deleteImportedImages(decisions.map { it.product })
            } catch (cleanupFailure: IOException) {
                failure.addSuppressed(cleanupFailure)
            }
            throw failure
        }
        val retainedImages = decisions
            .filter { it.action != DuplicateImportAction.SKIP }
            .flatMap { decision ->
                listOfNotNull(decision.product.imagePath) + decision.product.additionalImagePaths
            }
            .toSet()
        InventoryBackup.deleteImages(
            decisions.flatMap { decision ->
                listOfNotNull(decision.product.imagePath) + decision.product.additionalImagePaths
            }.filterNot { it in retainedImages } +
                replacedImagePaths
        )
    }

    private suspend fun insertInventoryBatch(
        dao: KhoDao,
        branchId: String,
        quantity: Double,
        unitCost: Long,
        at: Long,
        movementType: String
    ) {
        val batchId = UUID.randomUUID().toString()
        dao.insertBatch(
            StockBatchRecord(
                id = batchId,
                branchId = branchId,
                receivedAt = at,
                quantityReceived = quantity,
                quantityRemaining = quantity,
                unitCost = unitCost,
                approvalStatus = "APPROVED"
            )
        )
        dao.insertMovement(
            StockMovementRecord(
                id = UUID.randomUUID().toString(),
                branchId = branchId,
                type = movementType,
                quantityDelta = quantity,
                happenedAt = at,
                referenceId = batchId,
                syncStatus = "PENDING"
            )
        )
    }

    suspend fun receiveStock(branchId: String, quantity: Double, unitCost: Long) {
        require(quantity.isFinite() && quantity > 0.0) { "Số lượng nhập phải lớn hơn 0." }
        require(unitCost >= 0L) { "Giá nhập không hợp lệ." }

        val now = System.currentTimeMillis()
        database.withTransaction {
            val dao = database.dao()
            val branch = checkNotNull(dao.getBranch(branchId)) { "Không tìm thấy nhánh sản phẩm." }
            if (UnitType.valueOf(branch.unitType) == UnitType.PIECE) {
                require(quantity % 1.0 == 0.0) { "Số lượng Cái phải là số nguyên." }
            }
            check((branch.stock + quantity).isFinite()) { "Tồn kho vượt giới hạn." }
            val batchId = UUID.randomUUID().toString()
            dao.insertBatch(
                StockBatchRecord(
                    id = batchId,
                    branchId = branchId,
                    receivedAt = now,
                    quantityReceived = quantity,
                    quantityRemaining = quantity,
                    unitCost = unitCost,
                    approvalStatus = "APPROVED"
                )
            )
            dao.adjustStock(branchId, quantity)
            dao.updateBuyPrice(branchId, unitCost)
            dao.insertMovement(
                StockMovementRecord(
                    id = UUID.randomUUID().toString(),
                    branchId = branchId,
                    type = "RECEIVE",
                    quantityDelta = quantity,
                    happenedAt = now,
                    referenceId = batchId,
                    syncStatus = "PENDING"
                )
            )
        }
    }

    suspend fun updateSellPrice(branchId: String, price: Long) {
        require(price >= 0L) { "Giá bán không hợp lệ." }
        database.withTransaction {
            checkNotNull(database.dao().getBranch(branchId)) { "Không tìm thấy nhánh sản phẩm." }
            database.dao().updateSellPrice(branchId, price)
        }
    }

    suspend fun addUnitBranch(productId: String, unitType: UnitType, customName: String) {
        val name = when (unitType) {
            UnitType.PIECE -> "Cái"
            UnitType.WEIGHT -> "Kg"
            UnitType.OTHER -> customName.trim().replaceFirstChar { it.uppercase() }
        }
        require(name.isNotBlank()) { "Nhập tên đơn vị." }
        database.withTransaction {
            val dao = database.dao()
            checkNotNull(dao.getProduct(productId)) { "Không tìm thấy sản phẩm." }
            require(dao.findBranchByName(productId, name) == null) { "Đơn vị này đã có trong sản phẩm." }
            dao.insertBranch(
                UnitBranchRecord(
                    id = UUID.randomUUID().toString(),
                    productId = productId,
                    name = name,
                    unitType = unitType.name,
                    stock = 0.0,
                    buyPrice = 0L,
                    sellPrice = 0L,
                    lowStockThreshold = if (unitType == UnitType.WEIGHT) 10.0 else 2.0
                )
            )
        }
    }

    suspend fun createSale(
        customerName: String,
        lineItems: List<SaleLineInput>,
        initialPayment: Long
    ): String {
        require(lineItems.isNotEmpty()) { "Thêm ít nhất một sản phẩm." }
        require(initialPayment >= 0L) { "Số tiền thanh toán không hợp lệ." }
        require(lineItems.none {
            !it.quantity.isFinite() || it.quantity <= 0.0 || it.unitPrice < 0L
        }) { "Kiểm tra lại số lượng và giá bán." }

        val orderId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val total = lineItems.fold(0L) { amount, item ->
            Math.addExact(amount, calculateAmount(item.quantity, item.unitPrice))
        }
        require(initialPayment <= total) { "Tiền thu không được vượt tổng đơn hàng." }

        database.withTransaction {
            val dao = database.dao()
            dao.insertOrder(
                SalesOrderRecord(
                    id = orderId,
                    customerName = customerName.trim().ifBlank { "Khách lẻ" },
                    createdAt = now,
                    completedAt = if (initialPayment >= total && total > 0L) now else null,
                    status = when {
                        initialPayment >= total && total > 0L -> "COMPLETED"
                        initialPayment > 0L -> "PARTIALLY_PAID"
                        else -> "UNPAID"
                    },
                    totalAmount = total,
                    paidAmount = initialPayment
                )
            )
            for ((position, line) in lineItems.withIndex()) {
                val branch = checkNotNull(dao.getBranch(line.branchId)) { "Không tìm thấy sản phẩm." }
                if (UnitType.valueOf(branch.unitType) == UnitType.PIECE) {
                    require(line.quantity % 1.0 == 0.0) { "Số lượng Cái phải là số nguyên." }
                }
                check(line.quantity <= branch.stock) { "Không đủ tồn kho cho ${line.productName}." }
                check((branch.stock - line.quantity).isFinite()) { "Tồn kho vượt giới hạn." }
                val lineId = UUID.randomUUID().toString()
                dao.insertOrderLine(
                    SalesOrderLineRecord(
                        id = lineId,
                        orderId = orderId,
                        branchId = line.branchId,
                        productName = line.productName,
                        branchName = line.branchName,
                        quantity = line.quantity,
                        unitPrice = line.unitPrice,
                    lineTotal = calculateAmount(line.quantity, line.unitPrice),
                    position = position
                    )
                )
                var remaining = line.quantity
                for (batch in dao.getAvailableBatches(line.branchId)) {
                    if (remaining <= 0.0) break
                    val consumed = minOf(remaining, batch.quantityRemaining)
                    dao.consumeBatch(batch.id, consumed)
                    dao.insertAllocation(
                        OrderLineBatchAllocationRecord(
                            id = UUID.randomUUID().toString(),
                            orderLineId = lineId,
                            batchId = batch.id,
                            quantity = consumed,
                            unitCost = batch.unitCost
                        )
                    )
                    remaining -= consumed
                }
                dao.adjustStock(line.branchId, -line.quantity)
                dao.insertMovement(
                    StockMovementRecord(
                        id = UUID.randomUUID().toString(),
                        branchId = line.branchId,
                        type = "SALE",
                        quantityDelta = -line.quantity,
                        happenedAt = now,
                        referenceId = orderId,
                        syncStatus = "PENDING"
                    )
                )
            }
            if (initialPayment > 0L) {
                dao.insertPayment(
                    OrderPaymentRecord(
                        id = UUID.randomUUID().toString(),
                        orderId = orderId,
                        amount = initialPayment,
                        happenedAt = now,
                        kind = "PAYMENT"
                    )
                )
            }
        }
        return orderId
    }

    suspend fun addPayment(orderId: String, amount: Long) {
        require(amount > 0L) { "Số tiền thu phải lớn hơn 0." }
        database.withTransaction {
            val dao = database.dao()
            val order = checkNotNull(dao.getOrder(orderId)) { "Không tìm thấy đơn hàng." }
            check(order.status != "CANCELLED") { "Đơn hàng đã hủy." }
            val paid = Math.addExact(order.paidAmount, amount)
            check(paid <= order.totalAmount) { "Số tiền thu vượt số còn nợ." }
            val completed = paid >= order.totalAmount
            val now = System.currentTimeMillis()
            dao.insertPayment(
                OrderPaymentRecord(
                    id = UUID.randomUUID().toString(),
                    orderId = orderId,
                    amount = amount,
                    happenedAt = now,
                    kind = "PAYMENT"
                )
            )
            dao.updateOrderPayment(
                orderId = orderId,
                paidAmount = paid,
                status = if (completed) "COMPLETED" else "PARTIALLY_PAID",
                completedAt = if (completed) now else null
            )
        }
    }

    suspend fun cancelOrder(orderId: String) {
        database.withTransaction {
            val dao = database.dao()
            val order = checkNotNull(dao.getOrder(orderId)) { "Không tìm thấy đơn hàng." }
            check(order.status != "CANCELLED") { "Đơn hàng đã được hủy." }
            val now = System.currentTimeMillis()
            for (line in dao.getOrderLines(orderId)) {
                val branch = checkNotNull(dao.getBranch(line.branchId)) { "Không tìm thấy nhánh sản phẩm." }
                check((branch.stock + line.quantity).isFinite()) { "Tồn kho vượt giới hạn khi hoàn đơn." }
                val allocations = dao.getAllocations(line.id)
                val allocatedQuantity = allocations.sumOf { it.quantity }
                allocations.forEach { dao.restoreBatch(it.batchId, it.quantity) }
                val uncostedQuantity = (line.quantity - allocatedQuantity).coerceAtLeast(0.0)
                if (uncostedQuantity > 0.0) {
                    dao.insertBatch(
                        StockBatchRecord(
                            id = UUID.randomUUID().toString(),
                            branchId = line.branchId,
                            receivedAt = now,
                            quantityReceived = uncostedQuantity,
                            quantityRemaining = uncostedQuantity,
                            unitCost = branch.buyPrice,
                            approvalStatus = "APPROVED"
                        )
                    )
                }
                dao.adjustStock(line.branchId, line.quantity)
                dao.insertMovement(
                    StockMovementRecord(
                        id = UUID.randomUUID().toString(),
                        branchId = line.branchId,
                        type = "CANCEL_SALE",
                        quantityDelta = line.quantity,
                        happenedAt = now,
                        referenceId = orderId,
                        syncStatus = "PENDING"
                    )
                )
            }
            dao.cancelOrder(orderId, order.paidAmount)
        }
    }

    suspend fun confirmRefund(orderId: String) {
        database.withTransaction {
            val dao = database.dao()
            val order = checkNotNull(dao.getOrder(orderId)) { "Không tìm thấy đơn hàng." }
            check(order.status == "CANCELLED") { "Chỉ xác nhận hoàn tiền cho đơn đã hủy." }
            check(order.refundPending > 0L) { "Đơn hàng không có khoản hoàn tiền đang chờ." }
            dao.insertPayment(
                OrderPaymentRecord(
                    id = UUID.randomUUID().toString(),
                    orderId = orderId,
                    amount = order.refundPending,
                    happenedAt = System.currentTimeMillis(),
                    kind = "REFUND"
                )
            )
            dao.clearPendingRefund(orderId)
        }
    }
}

data class SaleLineInput(
    val branchId: String,
    val productName: String,
    val branchName: String,
    val quantity: Double,
    val unitPrice: Long
)

private fun SalesOrderRecord.toModel() = SalesOrder(
    id = id,
    customerName = customerName,
    createdAt = createdAt,
    completedAt = completedAt,
    status = status,
    totalAmount = totalAmount,
    paidAmount = paidAmount,
    refundPending = refundPending
)

private fun ProductWithBranches.toModel() = Product(
    id = product.id,
    name = product.name,
    active = product.active,
    imagePath = product.imagePath,
    additionalImagePaths = product.additionalImagePaths.decodeImagePaths(),
    createdAt = product.createdAt,
    unitSummaryLabel = product.unitSummaryLabel,
    branches = branches.map { branch ->
        UnitBranch(
            id = branch.id,
            name = branch.name,
            stock = branch.stock,
            unitType = UnitType.valueOf(branch.unitType),
            buyPrice = branch.buyPrice,
            sellPrice = branch.sellPrice,
            lowStockThreshold = branch.lowStockThreshold
        )
    }
)

private fun normalizeProductName(name: String): String =
    name.trim().replace(Regex("\\s+"), " ")

private fun List<String>.encodeImagePaths(): String = JSONArray(this).toString()

private fun String.decodeImagePaths(): List<String> {
    val paths = JSONArray(this)
    return List(paths.length()) { index -> paths.getString(index) }
}

private fun deleteProductImages(paths: List<String>) {
    InventoryBackup.deleteImages(paths)
}
