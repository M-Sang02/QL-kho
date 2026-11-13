package com.kho.glass.ui

import android.app.Application
import android.database.sqlite.SQLiteException
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.kho.glass.data.LocalStore
import com.kho.glass.data.ArchivedProductCleanupWorker
import com.kho.glass.data.FirestoreWarehouseSync
import com.kho.glass.data.InventoryImportDecision
import com.kho.glass.data.Product
import com.kho.glass.data.SaleLineInput
import com.kho.glass.data.SalesOrder
import com.kho.glass.data.SalesOrderLine
import com.kho.glass.data.StockMovementView
import com.kho.glass.data.UnitType
import com.kho.glass.data.calculateAmount
import com.kho.glass.data.parseCurrencyInput
import java.io.IOException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SaleDraftLine(
    val branchId: String,
    val productName: String,
    val branchName: String,
    val quantity: Double,
    val unitPrice: Long,
    val unitType: UnitType
) {
    val total: Long get() = calculateAmount(quantity, unitPrice)
}

data class ProductSnapshot(
    val products: List<Product>,
    val loaded: Boolean
)

class KhoViewModel(
    application: Application,
    userId: String,
    warehouseId: String,
    private val accountUid: String?,
    private val remoteWarehouseId: String
) : AndroidViewModel(application) {
    private val storeUserId = userId
    private val localWarehouseId = warehouseId
    private val store = LocalStore(application, userId, warehouseId)

    init {
        ArchivedProductCleanupWorker.schedule(
            application,
            userId,
            warehouseId,
            accountUid,
            remoteWarehouseId
        )
        viewModelScope.launch {
            kotlinx.coroutines.yield()
            try {
                if (store.deleteExpiredArchivedProducts() > 0) {
                    message = "Đã tự động xóa sản phẩm ẩn quá 30 ngày và ảnh liên quan."
                    accountUid?.let {
                        FirestoreWarehouseSync.schedule(
                            application,
                            it,
                            userId,
                            warehouseId,
                            remoteWarehouseId
                        )
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                message = "Không dọn được sản phẩm đã ẩn: ${error.localizedMessage ?: "lỗi tệp"}"
            } catch (error: SQLiteException) {
                message = "Không dọn được sản phẩm đã ẩn: ${error.localizedMessage ?: "lỗi cơ sở dữ liệu"}"
            } catch (error: IllegalStateException) {
                message = error.message ?: "Không dọn được sản phẩm đã ẩn."
            }
        }
        accountUid?.let {
            FirestoreWarehouseSync.schedule(
                application,
                it,
                userId,
                warehouseId,
                remoteWarehouseId
            )
        }
    }

    val productSnapshot = store.products
        .map { ProductSnapshot(products = it, loaded = true) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = ProductSnapshot(products = emptyList(), loaded = false)
        )
    val products = productSnapshot.map { it.products }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = emptyList()
    )
    val archivedProducts = store.archivedProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val orders = store.orders.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )
    val stockMovements = store.stockMovements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList<StockMovementView>()
    )

    var query by mutableStateOf("")
    var addDialog by mutableStateOf(false)
    var selectedBranchId by mutableStateOf("")
    var saleQuantity by mutableStateOf("1")
    var saleQuantityUnit by mutableStateOf("kg")
    var saleUnitPrice by mutableStateOf("")
    var saleCustomer by mutableStateOf("")
    var salePayment by mutableStateOf("")
    var message by mutableStateOf<String?>(null)
    val saleLines = mutableStateListOf<SaleDraftLine>()

    fun saveProduct(
        name: String,
        unitType: UnitType,
        openingStock: String,
        buyPrice: String,
        sellPrice: String,
        imagePath: String?,
        additionalImagePaths: List<String> = emptyList()
    ) = performAction {
        store.addProduct(
            name = name,
            unitType = unitType,
            openingStock = openingStock.toDoubleOrNull()
                ?: throw IllegalArgumentException("Tồn đầu kỳ không hợp lệ."),
            buyPrice = parseCurrencyInput(buyPrice)
                ?: throw IllegalArgumentException("Giá nhập không hợp lệ."),
            sellPrice = parseCurrencyInput(sellPrice)
                ?: throw IllegalArgumentException("Giá bán không hợp lệ."),
            imagePath = imagePath,
            additionalImagePaths = additionalImagePaths
        )
        addDialog = false
        message = "Đã thêm sản phẩm."
    }

    fun updateProduct(
        productId: String,
        name: String,
        imagePath: String?,
        additionalImagePaths: List<String>? = null
    ) = performAction {
        store.updateProduct(productId, name, imagePath, additionalImagePaths)
        message = "Đã cập nhật sản phẩm."
    }

    fun updateUnitSummaryLabel(productId: String, label: String) = performAction {
        store.updateUnitSummaryLabel(productId, label)
        message = if (label.isBlank()) "Đã ẩn nhãn loại sản phẩm." else "Đã đổi nhãn loại sản phẩm."
    }

    fun deactivateProduct(productId: String) = performAction {
        store.deactivateProduct(productId)
        message = "Đã ẩn sản phẩm; bạn có thể khôi phục trong mục Đã ẩn."
    }

    fun reactivateProduct(productId: String) = performAction {
        store.reactivateProduct(productId)
        message = "Đã khôi phục sản phẩm."
    }

    fun permanentlyDeleteProduct(productId: String) = performAction {
        store.permanentlyDeleteProduct(productId)
        message = "Đã xóa sản phẩm và ảnh khỏi danh sách kho; đơn hàng cũ vẫn được giữ."
    }

    fun hidePartialStock(productId: String, branchId: String, quantity: String) = performAction {
        store.hidePartialStock(
            productId,
            branchId,
            quantity.toDoubleOrNull() ?: throw IllegalArgumentException("Số lượng không hợp lệ.")
        )
        message = "Đã ẩn một phần tồn kho."
    }

    fun permanentlyDeleteAllProducts() = performAction {
        store.permanentlyDeleteAllProducts()
        message = "Đã xóa toàn bộ sản phẩm khỏi danh sách; lịch sử đơn và xuất nhập được giữ lại."
    }

    fun importInventory(decisions: List<InventoryImportDecision>) = performAction {
        store.importInventory(decisions)
        message = "Đã nhập dữ liệu kho; hàng bỏ qua không bị thay đổi và đơn cũ được giữ."
    }

    fun receiveStock(branchId: String, quantity: String, unitCost: String) = performAction {
        store.receiveStock(
            branchId = branchId,
            quantity = quantity.toDoubleOrNull() ?: throw IllegalArgumentException("Số lượng không hợp lệ."),
            unitCost = parseCurrencyInput(unitCost) ?: throw IllegalArgumentException("Giá nhập không hợp lệ.")
        )
        message = "Đã nhập hàng."
    }

    fun mergeDuplicateProduct(
        productName: String,
        branchId: String,
        quantity: String,
        unitCost: String
    ) = performAction {
        store.receiveStock(
            branchId = branchId,
            quantity = quantity.toDoubleOrNull()
                ?: throw IllegalArgumentException("Số lượng không hợp lệ."),
            unitCost = parseCurrencyInput(unitCost)
                ?: throw IllegalArgumentException("Giá nhập không hợp lệ.")
        )
        addDialog = false
        message = "Đã cộng tồn vào sản phẩm “$productName”; giá bán hiện tại được giữ nguyên."
    }

    fun updateSellPrice(branchId: String, price: String) = performAction {
        store.updateSellPrice(
            branchId,
            parseCurrencyInput(price) ?: throw IllegalArgumentException("Giá bán không hợp lệ.")
        )
        message = "Đã cập nhật giá bán."
    }

    fun addUnitBranch(productId: String, unitType: UnitType, customName: String) = performAction {
        store.addUnitBranch(productId, unitType, customName)
        message = "Đã thêm đơn vị."
    }

    fun addSaleLine() {
        val branch = products.value
            .asSequence()
            .flatMap { product -> product.branches.asSequence().map { product to it } }
            .firstOrNull { (_, unitBranch) -> unitBranch.id == selectedBranchId }
        if (branch == null) {
            message = "Chọn sản phẩm trước."
            return
        }
        val enteredQuantity = saleQuantity.toDoubleOrNull()
        val quantity = if (
            branch.second.unitType == UnitType.WEIGHT &&
            saleQuantityUnit.equals("g", ignoreCase = true)
        ) {
            enteredQuantity?.div(1000.0)
        } else {
            enteredQuantity
        }
        val price = if (saleUnitPrice.isBlank()) {
            branch.second.sellPrice
        } else {
            parseCurrencyInput(saleUnitPrice)
        }
        if (quantity == null || !quantity.isFinite() || quantity <= 0.0) {
            message = "Số lượng không hợp lệ."
            return
        }
        if (price == null || price < 0L) {
            message = "Giá bán không hợp lệ."
            return
        }
        saleLines += SaleDraftLine(
            branchId = branch.second.id,
            productName = branch.first.name,
            branchName = branch.second.name,
            quantity = quantity,
            unitPrice = price,
            unitType = branch.second.unitType
        )
        saleQuantity = "1"
        saleUnitPrice = ""
        message = null
    }

    fun removeSaleLine(index: Int) {
        if (index in saleLines.indices) saleLines.removeAt(index)
    }

    fun createSale() = performAction {
        store.createSale(
            customerName = saleCustomer,
            lineItems = saleLines.map {
                SaleLineInput(it.branchId, it.productName, it.branchName, it.quantity, it.unitPrice)
            },
            initialPayment = if (salePayment.isBlank()) {
                0L
            } else {
                parseCurrencyInput(salePayment)
                    ?: throw IllegalArgumentException("Số tiền thanh toán không hợp lệ.")
            }
        )
        saleLines.clear()
        saleCustomer = ""
        salePayment = ""
        message = "Đã lưu đơn hàng và cập nhật tồn kho."
    }

    fun receivePayment(orderId: String, amount: String) = performAction {
        store.addPayment(
            orderId = orderId,
            amount = parseCurrencyInput(amount) ?: throw IllegalArgumentException("Số tiền không hợp lệ.")
        )
        message = "Đã ghi nhận thanh toán."
    }

    fun cancelOrder(orderId: String) = performAction {
        store.cancelOrder(orderId)
        message = "Đã hủy đơn và hoàn lại tồn kho. Hãy xác nhận khi đã hoàn tiền."
    }

    fun confirmRefund(orderId: String) = performAction {
        store.confirmRefund(orderId)
        message = "Đã ghi nhận hoàn tiền."
    }

    fun observeOrderLines(orderId: String) = store.observeOrderLines(orderId)

    fun dismissMessage() {
        message = null
    }

    private fun performAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            message = null
            try {
                action()
                accountUid?.let {
                    FirestoreWarehouseSync.schedule(
                        getApplication<Application>(),
                        it,
                        storeUserId,
                        localWarehouseId,
                        remoteWarehouseId
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IllegalArgumentException) {
                message = error.message ?: "Dữ liệu không hợp lệ."
            } catch (error: IllegalStateException) {
                message = error.message ?: "Không thể hoàn tất thao tác."
            } catch (error: ArithmeticException) {
                message = "Giá trị vượt giới hạn có thể lưu."
            } catch (error: IOException) {
                message = "Không hoàn tất được thao tác tệp: ${error.localizedMessage ?: "lỗi tệp"}"
            } catch (error: SQLiteException) {
                message = "Không lưu được dữ liệu trên thiết bị: ${error.localizedMessage ?: "lỗi cơ sở dữ liệu"}"
            }
        }
    }
}

class KhoViewModelFactory(
    private val application: Application,
    private val userId: String,
    private val warehouseId: String,
    private val accountUid: String?,
    private val remoteWarehouseId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(KhoViewModel::class.java)) {
            "Unsupported ViewModel type: ${modelClass.name}"
        }
        return KhoViewModel(
            application,
            userId,
            warehouseId,
            accountUid,
            remoteWarehouseId
        ) as T
    }
}
