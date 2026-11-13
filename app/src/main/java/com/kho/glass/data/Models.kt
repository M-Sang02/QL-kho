package com.kho.glass.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

data class Product(
    val id: String,
    val name: String,
    val active: Boolean = true,
    val imagePath: String? = null,
    val additionalImagePaths: List<String> = emptyList(),
    val createdAt: Long = 0L,
    val unitSummaryLabel: String = "loại",
    val branches: List<UnitBranch> = emptyList()
)
data class UnitBranch(val id:String, val name:String, val stock:Double, val unitType:UnitType, val buyPrice:Long=0L, val sellPrice:Long=0L, val lowStockThreshold:Double=2.0)
enum class UnitType { PIECE, WEIGHT, OTHER }
data class DashboardStats(val stockValue:Long=0L,val exportedValue:Long=0L,val revenue:Long=0L,val receivable:Long=0L,val profit:Long=0L)

data class StockBatch(
    val id: String,
    val branchId: String,
    val receivedAt: Long,
    val quantityReceived: Double,
    val quantityRemaining: Double,
    val unitCost: Long,
    val approvalStatus: String
)

data class StockMovement(
    val id: String,
    val branchId: String,
    val type: String,
    val quantityDelta: Double,
    val happenedAt: Long,
    val referenceId: String?,
    val syncStatus: String
)

data class StockMovementView(
    val id: String,
    val branchId: String,
    val type: String,
    val quantityDelta: Double,
    val happenedAt: Long,
    val referenceId: String?,
    val syncStatus: String,
    val productName: String,
    val branchName: String
)

data class SalesOrder(
    val id: String,
    val customerName: String,
    val createdAt: Long,
    val completedAt: Long?,
    val status: String,
    val totalAmount: Long,
    val paidAmount: Long,
    val refundPending: Long = 0L
)

data class SalesOrderLine(
    val id: String,
    val orderId: String,
    val branchId: String,
    val productName: String,
    val branchName: String,
    val quantity: Double,
    val unitPrice: Long,
    val lineTotal: Long,
    val productDeleted: Boolean = false
)

data class OrderPayment(
    val id: String,
    val orderId: String,
    val amount: Long,
    val happenedAt: Long,
    val kind: String
)

fun calculateAmount(quantity: Double, unitPrice: Long): Long =
    BigDecimal.valueOf(quantity)
        .multiply(BigDecimal.valueOf(unitPrice))
        .setScale(0, RoundingMode.HALF_UP)
        .longValueExact()

fun parseCurrencyInput(value: String): Long? =
    value.filter(Char::isDigit).takeIf { it.isNotEmpty() }?.toLongOrNull()

fun formatCurrencyInput(value: String): String {
    val digits = value.filter(Char::isDigit)
    if (digits.isEmpty()) return ""
    return digits.toLongOrNull()?.let {
        NumberFormat.getIntegerInstance(Locale("vi", "VN")).format(it)
    } ?: digits
}
