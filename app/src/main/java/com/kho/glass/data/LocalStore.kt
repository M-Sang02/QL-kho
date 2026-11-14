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
import java.util.Calendar

@Entity(tableName = "products")
data class ProductRecord(
    @PrimaryKey val id: String,
    val name: String,
    val active: Boolean = true,
    val createdAt: Long,
    val imagePath: String? = null,
    val permanentlyDeleted: Boolean = false,
    val archivedAt: Long? = null,
    val deletedAt: Long? = null,
    val unitSummaryLabel: String = "loại",
    @ColumnInfo(defaultValue = "'[]'")
    val additionalImagePaths: String = "[]",
    @ColumnInfo(defaultValue = "0")
    val preDeleteActive: Boolean = false
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
    val lowStockThreshold: Double,
    @ColumnInfo(defaultValue = "0")
    val archivedStock: Double = 0.0
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
    @Query(
        "SELECT * FROM products WHERE permanentlyDeleted = 0 AND " +
            "(active = 0 OR EXISTS (SELECT 1 FROM unit_branches b " +
            "WHERE b.productId = products.id AND b.archivedStock > 0)) " +
            "ORDER BY name COLLATE NOCASE"
    )
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

    @Query(
        "SELECT id FROM products WHERE permanentlyDeleted = 1 " +
            "AND deletedAt IS NOT NULL AND deletedAt <= :cutoff"
    )
    suspend fun getExpiredPendingDeletionIds(cutoff: Long): List<String>

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
            "m.syncStatus, p.name AS productName, b.name AS branchName, b.unitType AS unitType, " +
            "p.permanentlyDeleted AS productDeleted " +
            "FROM stock_movements m " +
            "INNER JOIN unit_branches b ON b.id = m.branchId " +
            "INNER JOIN products p ON p.id = b.productId " +
            "WHERE m.type != 'UNHIDE_STOCK' " +
            "ORDER BY m.happenedAt DESC"
    )
    fun observeStockMovements(): Flow<List<StockMovementView>>

    @Query("SELECT * FROM sales_order_lines WHERE orderId = :orderId ORDER BY position")
    fun observeOrderLines(orderId: String): Flow<List<SalesOrderLineRecord>>

    @Query("SELECT * FROM sales_order_lines WHERE orderId = :orderId")
    suspend fun getOrderLines(orderId: String): List<SalesOrderLineRecord>

    @Query("UPDATE sales_order_lines SET productDeleted = :deleted WHERE branchId IN (:branchIds)")
    suspend fun setOrderLinesProductDeleted(branchIds: List<String>, deleted: Boolean)

    @Query(
        "UPDATE sales_order_lines SET productDeleted = :deleted WHERE branchId IN " +
            "(SELECT id FROM unit_branches WHERE productId IN " +
            "(SELECT id FROM products WHERE permanentlyDeleted = 0))"
    )
    suspend fun setAllOrderLinesProductsDeleted(deleted: Boolean)

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

    @Query("UPDATE products SET active = 0, archivedAt = :archivedAt WHERE id = :productId")
    suspend fun deactivateProduct(productId: String, archivedAt: Long)

    @Query("UPDATE products SET active = 1, archivedAt = NULL WHERE id = :productId")
    suspend fun reactivateProduct(productId: String)

    @Query(
        "UPDATE products SET preDeleteActive = active, active = 0, " +
            "permanentlyDeleted = 1, deletedAt = :deletedAt WHERE id = :productId"
    )
    suspend fun permanentlyDeleteProduct(productId: String, deletedAt: Long)

    @Query(
        "UPDATE products SET preDeleteActive = active, active = 0, " +
            "permanentlyDeleted = 1, deletedAt = :deletedAt WHERE permanentlyDeleted = 0"
    )
    suspend fun permanentlyDeleteAllProducts(deletedAt: Long)

    @Query(
        "SELECT * FROM products WHERE permanentlyDeleted = 0 AND archivedAt IS NOT NULL " +
            "AND archivedAt <= :cutoff AND (active = 0 OR EXISTS " +
            "(SELECT 1 FROM unit_branches b WHERE b.productId = products.id AND b.archivedStock > 0))"
    )
    suspend fun getExpiredArchivedProducts(cutoff: Long): List<ProductRecord>

    @Query("UPDATE products SET active = preDeleteActive, permanentlyDeleted = 0, deletedAt = NULL, preDeleteActive = 0 WHERE id = :productId")
    suspend fun undoPermanentDelete(productId: String)

    @Query("UPDATE products SET imagePath = NULL, additionalImagePaths = '[]' WHERE id = :productId")
    suspend fun clearDeletedProductImages(productId: String)

    @Query("UPDATE products SET permanentlyDeleted = 0, deletedAt = NULL, active = 1, archivedAt = NULL WHERE id = :productId")
    suspend fun restoreExpiredProduct(productId: String)

    @Query("UPDATE products SET archivedAt = :archivedAt WHERE id = :productId")
    suspend fun updateArchivedAt(productId: String, archivedAt: Long)

    @Query("UPDATE unit_branches SET archivedStock = archivedStock + :delta WHERE id = :branchId")
    suspend fun adjustArchivedStock(branchId: String, delta: Double)

    @Query("UPDATE unit_branches SET stock = 0, archivedStock = 0 WHERE productId = :productId")
    suspend fun clearProductStock(productId: String)

    @Query("UPDATE stock_batches SET quantityRemaining = 0 WHERE branchId IN (SELECT id FROM unit_branches WHERE productId = :productId)")
    suspend fun clearProductBatchStock(productId: String)

    @Query(
        "SELECT * FROM stock_movements WHERE branchId = :branchId " +
            "AND type = 'ARCHIVE_STOCK' AND referenceId = :referenceId LIMIT 1"
    )
    suspend fun getArchiveMovement(branchId: String, referenceId: String): StockMovementRecord?

    @Query("UPDATE stock_movements SET quantityDelta = :quantityDelta WHERE id = :movementId")
    suspend fun updateMovementQuantity(movementId: String, quantityDelta: Double)

    @Query(
        "UPDATE stock_movements SET type = :newType WHERE " +
            "type IN ('ARCHIVE_STOCK', 'RESTORE_ARCHIVE', 'HIDE_PRODUCT', 'PARTIAL_HIDE') AND " +
            "(referenceId = :productId OR referenceId LIKE :archivePrefix)"
    )
    suspend fun updateArchiveMovementType(
        productId: String,
        archivePrefix: String,
        newType: String
    )

    @Query(
        "UPDATE stock_movements SET type = 'ARCHIVE_STOCK' " +
            "WHERE type = 'DELETE_PRODUCT' AND " +
            "(referenceId = :productId OR referenceId LIKE :archivePrefix)"
    )
    suspend fun restoreArchiveMovementType(productId: String, archivePrefix: String)

    @Query(
        "DELETE FROM stock_movements WHERE type = 'DELETE_PRODUCT' " +
            "AND referenceId LIKE :deletePrefix"
    )
    suspend fun deleteUndoableDeleteMovements(deletePrefix: String)

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
    version = 7,
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

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE products ADD COLUMN archivedAt INTEGER")
                db.execSQL("ALTER TABLE products ADD COLUMN deletedAt INTEGER")
                db.execSQL(
                    "UPDATE products SET archivedAt = strftime('%s','now') * 1000 " +
                        "WHERE active = 0 AND permanentlyDeleted = 0"
                )
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE unit_branches ADD COLUMN archivedStock REAL NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "ALTER TABLE products ADD COLUMN preDeleteActive INTEGER NOT NULL DEFAULT 0"
                )
                migrateLegacyArchivedStock(db)
                db.execSQL(
                    """
                    UPDATE unit_branches SET stock = 0 WHERE productId IN (
                        SELECT id FROM products WHERE active = 0 AND permanentlyDeleted = 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    UPDATE stock_batches SET quantityRemaining = 0 WHERE branchId IN (
                        SELECT b.id FROM unit_branches b
                        INNER JOIN products p ON p.id = b.productId
                        WHERE p.active = 0 AND p.permanentlyDeleted = 0
                    )
                    """.trimIndent()
                )
            }
        }

        private fun migrateLegacyArchivedStock(db: SupportSQLiteDatabase) {
            data class ArchiveKey(val branchId: String, val dayStart: Long)
            data class ArchiveGroup(
                val productId: String,
                var quantityDelta: Double,
                var happenedAt: Long
            )

            val activeByProduct = mutableMapOf<String, Boolean>()
            val archivedAtByProduct = mutableMapOf<String, Long>()
            val permanentlyDeletedProducts = mutableSetOf<String>()
            db.query(
                "SELECT id, active, permanentlyDeleted, archivedAt FROM products"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val productId = cursor.getString(0)
                    activeByProduct[productId] = cursor.getInt(1) != 0
                    if (cursor.getInt(2) != 0) permanentlyDeletedProducts += productId
                    if (!cursor.isNull(3)) archivedAtByProduct[productId] = cursor.getLong(3)
                }
            }

            val groups = mutableMapOf<ArchiveKey, ArchiveGroup>()
            val latestArchiveAt = mutableMapOf<String, Long>()
            val latestPartialHideAt = mutableMapOf<String, Long>()
            val legacyMovementIds = mutableListOf<String>()
            db.query(
                "SELECT m.id, m.branchId, b.productId, m.type, " +
                    "m.quantityDelta, m.happenedAt " +
                    "FROM stock_movements m INNER JOIN unit_branches b ON b.id = m.branchId " +
                    "WHERE m.type IN ('PARTIAL_HIDE', 'HIDE_PRODUCT')"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val movementId = cursor.getString(0)
                    val branchId = cursor.getString(1)
                    val productId = cursor.getString(2)
                    val type = cursor.getString(3)
                    val quantityDelta = cursor.getDouble(4)
                    val happenedAt = cursor.getLong(5)
                    if (productId in permanentlyDeletedProducts) continue
                    latestArchiveAt[productId] = maxOf(
                        latestArchiveAt[productId] ?: Long.MIN_VALUE,
                        happenedAt
                    )
                    if (type == "HIDE_PRODUCT" && activeByProduct[productId] == true) {
                        continue
                    }
                    legacyMovementIds += movementId
                    if (type != "PARTIAL_HIDE") continue
                    latestPartialHideAt[productId] = maxOf(
                        latestPartialHideAt[productId] ?: Long.MIN_VALUE,
                        happenedAt
                    )

                    val key = ArchiveKey(branchId, archiveDayStart(happenedAt))
                    val group = groups[key]
                    if (group == null) {
                        groups[key] = ArchiveGroup(productId, quantityDelta, happenedAt)
                    } else {
                        group.quantityDelta += quantityDelta
                        group.happenedAt = minOf(group.happenedAt, happenedAt)
                    }
                }
            }

            db.query(
                "SELECT b.id, b.productId, b.stock FROM unit_branches b " +
                    "INNER JOIN products p ON p.id = b.productId " +
                    "WHERE p.active = 0 AND p.permanentlyDeleted = 0 AND b.stock > 0"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val branchId = cursor.getString(0)
                    val productId = cursor.getString(1)
                    val hiddenAt = latestArchiveAt[productId]
                        ?: archivedAtByProduct[productId]
                        ?: System.currentTimeMillis()
                    val key = ArchiveKey(branchId, archiveDayStart(hiddenAt))
                    val group = groups[key]
                    if (group == null) {
                        groups[key] = ArchiveGroup(productId, -cursor.getDouble(2), hiddenAt)
                    } else {
                        group.quantityDelta -= cursor.getDouble(2)
                        group.happenedAt = minOf(group.happenedAt, hiddenAt)
                    }
                }
            }

            val archivedByBranch = mutableMapOf<String, Double>()
            val archivedByProduct = mutableMapOf<String, Double>()
            groups.forEach { (key, group) ->
                val values = android.content.ContentValues().apply {
                    put("id", UUID.randomUUID().toString())
                    put("branchId", key.branchId)
                    put("type", "ARCHIVE_STOCK")
                    put("quantityDelta", group.quantityDelta)
                    put("happenedAt", group.happenedAt)
                    put("referenceId", "ARCHIVE:${group.productId}:${key.dayStart}")
                    put("syncStatus", "PENDING")
                }
                db.insert(
                    "stock_movements",
                    android.database.sqlite.SQLiteDatabase.CONFLICT_ABORT,
                    values
                )
                val quantity = (-group.quantityDelta).coerceAtLeast(0.0)
                archivedByBranch[key.branchId] =
                    (archivedByBranch[key.branchId] ?: 0.0) + quantity
                archivedByProduct[group.productId] =
                    (archivedByProduct[group.productId] ?: 0.0) + quantity
            }
            legacyMovementIds.forEach { movementId ->
                db.delete("stock_movements", "id = ?", arrayOf(movementId))
            }
            archivedByBranch.forEach { (branchId, quantity) ->
                db.execSQL(
                    "UPDATE unit_branches SET archivedStock = ? WHERE id = ?",
                    arrayOf(quantity, branchId)
                )
            }
            activeByProduct.forEach { (productId, active) ->
                if (productId in permanentlyDeletedProducts) return@forEach
                val archivedAt = when {
                    !active -> latestArchiveAt[productId]
                        ?: archivedAtByProduct[productId]
                        ?: System.currentTimeMillis()
                    (archivedByProduct[productId] ?: 0.0) > 0.0 ->
                        latestPartialHideAt[productId] ?: archivedAtByProduct[productId]
                    else -> null
                }
                if (archivedAt == null) {
                    db.execSQL(
                        "UPDATE products SET archivedAt = NULL WHERE id = ?",
                        arrayOf(productId)
                    )
                } else {
                    db.execSQL(
                        "UPDATE products SET archivedAt = ? WHERE id = ?",
                        arrayOf(archivedAt, productId)
                    )
                }
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
        KhoDatabase.MIGRATION_4_5,
        KhoDatabase.MIGRATION_5_6,
        KhoDatabase.MIGRATION_6_7
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
        val deletedImagePaths = mutableListOf<String>()
        database.withTransaction {
            val dao = database.dao()
            val existingProducts = dao.getAllProducts().associateBy { it.id }
            val mergedProducts = snapshot.products.map { product ->
                val localProduct = existingProducts[product.id]
                val finalDeletionComplete = product.permanentlyDeleted &&
                    (product.deletedAt ?: Long.MAX_VALUE) <= System.currentTimeMillis() - 5_000L
                if (finalDeletionComplete && localProduct != null) {
                    deletedImagePaths += listOfNotNull(localProduct.imagePath) +
                        localProduct.additionalImagePaths.decodeImagePaths()
                }
                product.copy(
                    imagePath = if (finalDeletionComplete) null
                    else localProduct?.imagePath,
                    additionalImagePaths = if (finalDeletionComplete) "[]"
                    else localProduct?.additionalImagePaths ?: "[]"
                )
            }
            val movementTotals = snapshot.movements
                .filterNot { it.type == "DELETE_PRODUCT" }
                .groupBy { it.branchId }
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
            val mergedBatchRemaining = snapshot.batches.associate { batch ->
                batch.id to (
                    batch.quantityReceived -
                        (allocatedByBatch[batch.id] ?: 0.0).coerceAtMost(batch.quantityReceived)
                    )
            }.toMutableMap()
            val nonSaleDeductions = snapshot.movements
                .filter {
                    it.type in setOf("PARTIAL_HIDE", "ARCHIVE_STOCK", "RESTORE_ARCHIVE", "UNHIDE_STOCK") ||
                        it.type == "DELETE_STOCK" ||
                        (it.type == "RESTORE" && it.quantityDelta < 0.0)
                }
                .groupBy { it.branchId }
                .mapValues { (_, movements) ->
                    movements.sumOf { -it.quantityDelta }.coerceAtLeast(0.0)
                }
            snapshot.batches.groupBy { it.branchId }.forEach { (branchId, batches) ->
                var remainingToDeduct = nonSaleDeductions[branchId] ?: 0.0
                for (batch in batches.sortedWith(compareBy<StockBatchRecord> { it.receivedAt }
                    .thenBy { it.id }).asReversed()) {
                    if (remainingToDeduct <= 1e-8) break
                    val available = mergedBatchRemaining.getValue(batch.id).coerceAtLeast(0.0)
                    val deduction = minOf(remainingToDeduct, available)
                    mergedBatchRemaining[batch.id] = available - deduction
                    remainingToDeduct -= deduction
                }
                check(remainingToDeduct <= 1e-8) {
                    "Không thể đồng bộ tồn kho: số lượng ẩn vượt quá số dư lô hàng."
                }
            }
            val mergedBatches = snapshot.batches.map { batch ->
                batch.copy(
                    quantityRemaining = mergedBatchRemaining.getValue(batch.id)
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
        deleteProductImages(deletedImagePaths.distinct())
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
        require((if (imagePath == null) 0 else 1) + additionalImagePaths.size <= 10) {
            "Mỗi sản phẩm có thể có tối đa 10 ảnh, tính cả ảnh chính."
        }
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
            require((if (imagePath == null) 0 else 1) + retainedAdditionalImages.size <= 10) {
                "Mỗi sản phẩm có thể có tối đa 10 ảnh, tính cả ảnh chính."
            }
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
        val now = System.currentTimeMillis()
        database.withTransaction {
            val dao = database.dao()
            val product = checkNotNull(dao.getProduct(productId)) { "Không tìm thấy sản phẩm." }
            check(!product.permanentlyDeleted) { "Sản phẩm đã bị xóa." }
            if (product.active) {
                val dayStart = archiveDayStart(now)
                val archiveTime = product.archivedAt?.takeIf {
                    archiveDayStart(it) == dayStart
                } ?: dayStart
                dao.deactivateProduct(productId, archiveTime)
                dao.updateArchivedAt(productId, archiveTime)
                for (branch in dao.getProductBranches(productId)) {
                    val quantity = branch.stock
                    if (quantity <= 0.0) continue
                    removeAvailableBatches(dao, branch.id, quantity)
                    dao.adjustStock(branch.id, -quantity)
                    dao.adjustArchivedStock(branch.id, quantity)
                    recordArchiveMovement(dao, productId, branch.id, quantity, now, dayStart)
                }
            }
        }
    }

    suspend fun reactivateProduct(productId: String) {
        database.withTransaction {
            val dao = database.dao()
            val product = checkNotNull(dao.getProduct(productId)) { "Không tìm thấy sản phẩm." }
            check(!product.permanentlyDeleted) { "Sản phẩm đã bị xóa." }
            val duplicate = !product.active && dao.getActiveProducts().any {
                it.id != productId &&
                normalizeProductName(it.name).equals(
                    normalizeProductName(product.name),
                    ignoreCase = true
                )
            }
            require(!duplicate) { "Đã có sản phẩm đang dùng tên này." }
            val branches = dao.getProductBranches(productId)
            branches.forEach { branch ->
                val quantity = branch.archivedStock
                if (quantity <= 0.0) return@forEach
                val now = System.currentTimeMillis()
                dao.adjustArchivedStock(branch.id, -quantity)
                dao.adjustStock(branch.id, quantity)
                dao.insertBatch(
                    StockBatchRecord(
                        id = UUID.randomUUID().toString(),
                        branchId = branch.id,
                        receivedAt = now,
                        quantityReceived = quantity,
                        quantityRemaining = quantity,
                        unitCost = branch.buyPrice,
                        approvalStatus = "APPROVED"
                    )
                )
                dao.insertMovement(
                    StockMovementRecord(
                        id = UUID.randomUUID().toString(),
                        branchId = branch.id,
                        type = "UNHIDE_STOCK",
                        quantityDelta = quantity,
                        happenedAt = now,
                        referenceId = productId,
                        syncStatus = "PENDING"
                    )
                )
            }
            dao.updateArchiveMovementType(
                productId,
                "ARCHIVE:$productId:%",
                "RESTORE_ARCHIVE"
            )
            if (!product.active || branches.any { it.archivedStock > 0.0 }) {
                dao.reactivateProduct(productId)
            }
        }
    }

    suspend fun permanentlyDeleteProduct(productId: String) {
        database.withTransaction {
            val dao = database.dao()
            val product = checkNotNull(dao.getProduct(productId)) { "Không tìm thấy sản phẩm." }
            if (!product.permanentlyDeleted) {
                val now = System.currentTimeMillis()
                dao.permanentlyDeleteProduct(productId, now)
            }
        }
    }

    suspend fun permanentlyDeleteProducts(productIds: List<String>) {
        productIds.distinct().forEach { permanentlyDeleteProduct(it) }
    }

    suspend fun undoPermanentlyDeletedProducts(productIds: List<String>) {
        database.withTransaction {
            val dao = database.dao()
            for (productId in productIds.distinct()) {
                val product = dao.getProduct(productId) ?: continue
                if (!product.permanentlyDeleted || product.deletedAt == null) continue
                check(System.currentTimeMillis() - product.deletedAt < 5_000L) {
                    "Thời gian hoàn tác đã hết."
                }
                dao.undoPermanentDelete(productId)
                dao.deleteUndoableDeleteMovements("DELETE:$productId:%")
            }
        }
    }

    suspend fun finalizePermanentDeletion(productIds: List<String>) {
        val imagePaths = mutableListOf<String>()
        database.withTransaction {
            val dao = database.dao()
            for (productId in productIds.distinct()) {
                val product = dao.getProduct(productId) ?: continue
                if (!product.permanentlyDeleted || product.deletedAt == null) continue
                val branches = dao.getProductBranches(productId)
                dao.setOrderLinesProductDeleted(branches.map { it.id }, true)
                branches.forEach { branch ->
                    if (branch.stock > 0.0) {
                        dao.insertMovement(
                            StockMovementRecord(
                                id = UUID.randomUUID().toString(),
                                branchId = branch.id,
                                type = "DELETE_STOCK",
                                quantityDelta = -branch.stock,
                                happenedAt = System.currentTimeMillis(),
                                referenceId = productId,
                                syncStatus = "PENDING"
                            )
                        )
                    }
                }
                dao.deleteUndoableDeleteMovements("DELETE:$productId:%")
                imagePaths += listOfNotNull(product.imagePath) +
                    product.additionalImagePaths.decodeImagePaths()
                dao.clearProductStock(productId)
                dao.clearProductBatchStock(productId)
                dao.clearDeletedProductImages(productId)
            }
        }
        deleteProductImages(imagePaths.distinct())
    }

    suspend fun permanentlyDeleteAllProducts() {
        val productIds = mutableListOf<String>()
        database.withTransaction {
            val dao = database.dao()
            val now = System.currentTimeMillis()
            val products = dao.getProductsForPermanentDeletion()
            productIds += products.map { it.id }
            dao.permanentlyDeleteAllProducts(now)
        }
        finalizePermanentDeletion(productIds)
    }

    suspend fun hidePartialStock(productId: String, branchId: String, quantity: Double) {
        require(quantity.isFinite() && quantity > 0.0) { "Nhập số lượng cần ẩn lớn hơn 0." }
        database.withTransaction {
            val dao = database.dao()
            val product = checkNotNull(dao.getProduct(productId)) { "Không tìm thấy sản phẩm." }
            check(!product.permanentlyDeleted) {
                "Sản phẩm đã bị xóa."
            }
            val branch = checkNotNull(dao.getBranch(branchId)) { "Không tìm thấy đơn vị sản phẩm." }
            require(branch.productId == productId) { "Đơn vị không thuộc sản phẩm này." }
            if (UnitType.valueOf(branch.unitType) == UnitType.PIECE) {
                require(quantity % 1.0 == 0.0) { "Số lượng Cái phải là số nguyên." }
            }
            require(quantity <= branch.stock) { "Số lượng ẩn vượt quá tồn kho hiện có." }

            removeAvailableBatches(dao, branchId, quantity)
            val now = System.currentTimeMillis()
            val dayStart = archiveDayStart(now)
            val archiveTime = product.archivedAt?.takeIf {
                archiveDayStart(it) == dayStart
            } ?: dayStart
            dao.adjustStock(branchId, -quantity)
            dao.adjustArchivedStock(branchId, quantity)
            dao.updateArchivedAt(productId, archiveTime)
            recordArchiveMovement(dao, productId, branchId, quantity, now, dayStart)
        }
    }

    suspend fun deleteExpiredArchivedProducts(): Int {
        val today = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val cutoff = (today.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -30)
            add(Calendar.DAY_OF_YEAR, 1)
            add(Calendar.MILLISECOND, -1)
        }.timeInMillis
        val expiredProductIds = database.dao().getExpiredArchivedProducts(cutoff).map { it.id }
        if (expiredProductIds.isNotEmpty()) {
            permanentlyDeleteProducts(expiredProductIds)
            finalizePermanentDeletion(expiredProductIds)
        }
        val pendingCutoff = System.currentTimeMillis() - 5_000L
        val pendingDeleteIds = database.dao().getExpiredPendingDeletionIds(pendingCutoff)
        finalizePermanentDeletion(pendingDeleteIds)
        return expiredProductIds.size + pendingDeleteIds.size
    }

    private suspend fun recordArchiveMovement(
        dao: KhoDao,
        productId: String,
        branchId: String,
        quantity: Double,
        happenedAt: Long,
        dayStart: Long
    ) {
        val referenceId = "ARCHIVE:$productId:$dayStart"
        val existing = dao.getArchiveMovement(branchId, referenceId)
        if (existing != null) {
            dao.updateMovementQuantity(existing.id, existing.quantityDelta - quantity)
        } else {
            dao.insertMovement(
                StockMovementRecord(
                    id = UUID.randomUUID().toString(),
                    branchId = branchId,
                    type = "ARCHIVE_STOCK",
                    quantityDelta = -quantity,
                    happenedAt = happenedAt,
                    referenceId = referenceId,
                    syncStatus = "PENDING"
                )
            )
        }
    }

    private suspend fun removeAvailableBatches(dao: KhoDao, branchId: String, quantity: Double) {
        var remaining = quantity
        for (batch in dao.getAvailableBatches(branchId).asReversed()) {
            if (remaining <= 1e-8) break
            val removed = minOf(remaining, batch.quantityRemaining)
            dao.consumeBatch(batch.id, removed)
            remaining -= removed
        }
        check(remaining <= 1e-8) { "Không đủ lô hàng để cập nhật tồn kho." }
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
    archivedAt = product.archivedAt,
    unitSummaryLabel = product.unitSummaryLabel,
    branches = branches.map { branch ->
        UnitBranch(
            id = branch.id,
            name = branch.name,
            stock = branch.stock,
            unitType = UnitType.valueOf(branch.unitType),
            buyPrice = branch.buyPrice,
            sellPrice = branch.sellPrice,
            lowStockThreshold = branch.lowStockThreshold,
            archivedStock = branch.archivedStock
        )
    }
)

private fun normalizeProductName(name: String): String =
    name.trim().replace(Regex("\\s+"), " ")

private fun archiveDayStart(timestamp: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = timestamp
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun List<String>.encodeImagePaths(): String = JSONArray(this).toString()

private fun String.decodeImagePaths(): List<String> {
    val paths = JSONArray(this)
    return List(paths.length()) { index -> paths.getString(index) }
}

private fun deleteProductImages(paths: List<String>) {
    InventoryBackup.deleteImages(paths)
}
