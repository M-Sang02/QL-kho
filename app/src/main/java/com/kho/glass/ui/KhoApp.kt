package com.kho.glass.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Size
import android.os.Build
import android.os.SystemClock
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.kho.glass.data.InventoryBackup
import com.kho.glass.data.InventoryBackupProduct
import com.kho.glass.data.DuplicateImportAction
import com.kho.glass.data.InventoryImportDecision
import com.kho.glass.data.LocalStore
import com.kho.glass.data.StockMovementView
import com.kho.glass.data.Product
import com.kho.glass.data.SalesOrder
import com.kho.glass.data.UnitBranch
import com.kho.glass.data.UnitType
import com.kho.glass.data.calculateAmount
import com.kho.glass.data.formatCurrencyInput
import com.kho.glass.data.parseCurrencyInput
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Date
import java.util.Locale
import java.util.UUID
import org.json.JSONException
import org.json.JSONArray
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import android.content.ContentUris
import android.content.pm.PackageManager
import com.canhub.cropper.CropImageOptions
import com.canhub.cropper.CropImageView

private enum class ProductFilter {
    ALL,
    LOW_STOCK,
    ARCHIVED
}

private enum class SortField(val title: String) {
    NAME("Tên"),
    STOCK("Số lượng"),
    PRICE("Giá")
}

private enum class StockUnitFilter(val title: String) {
    PIECE("Cái"),
    WEIGHT("Kg"),
    ALL("Tất cả")
}

private data class GalleryImage(val uri: Uri, val name: String, val dateAdded: Long)

private data class WarehouseInfo(val id: String, val name: String, val cloudId: String? = null)

private const val DEFAULT_WAREHOUSE_ID = "default"

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun KhoApp(
    userId: String,
    localOnly: Boolean = false,
    accountDisplayName: String? = null,
    accountEmail: String? = null,
    accountKey: String = "local-device",
    accountPhotoUrl: String? = null,
    onRequestGoogleSignIn: (() -> Unit)? = null,
    onEnableCodeSync: (() -> Unit)? = null,
    onSignOut: (() -> Unit)? = null,
    sharedImageUri: Uri? = null,
    onSharedImageHandled: () -> Unit = {},
    sharedText: String? = null,
    onSharedTextHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val application = context.applicationContext as Application
    val preferences = remember(context) {
        context.getSharedPreferences("kho-glass-preferences", Context.MODE_PRIVATE)
    }
    val initialWarehouses = remember(preferences, userId) {
        loadWarehouses(preferences, userId)
    }
    var warehouses by remember(initialWarehouses) {
        mutableStateOf(initialWarehouses)
    }
    var selectedWarehouseId by remember(preferences, userId) {
        mutableStateOf(
            preferences.getString(warehouseSelectionKey(userId), DEFAULT_WAREHOUSE_ID)
                ?.takeIf { id -> initialWarehouses.any { it.id == id } }
                ?: initialWarehouses.first().id
        )
    }
    val selectedWarehouse = warehouses.firstOrNull { it.id == selectedWarehouseId }
        ?: warehouses.first()
    val appName = selectedWarehouse.name
    val remoteWarehouseId = selectedWarehouse.cloudId
        ?: UUID.nameUUIDFromBytes("$accountKey|$selectedWarehouseId".toByteArray()).toString()
    val syncErrorKey = com.kho.glass.data.FirestoreWarehouseSync.syncErrorKey(remoteWarehouseId)
    var syncError by remember(preferences, syncErrorKey) {
        mutableStateOf(preferences.getString(syncErrorKey, null))
    }
    DisposableEffect(preferences, syncErrorKey) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == syncErrorKey) syncError = preferences.getString(syncErrorKey, null)
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val warehousePreferencesPrefix = "warehouse_${selectedWarehouseId}_"
    var customAccountPhoto by remember(preferences, accountKey) {
        mutableStateOf(preferences.getString("account_photo_$accountKey", null))
    }
    val visibleAccountPhoto = customAccountPhoto ?: accountPhotoUrl
    val vm: KhoViewModel = viewModel(
        key = "${accountKey}_${userId}_$selectedWarehouseId",
        factory = KhoViewModelFactory(
            application,
            userId,
            selectedWarehouseId,
            accountUid = accountKey.takeUnless { localOnly || it == "local-device" },
            remoteWarehouseId = remoteWarehouseId
        )
    )
    val productSnapshot by vm.productSnapshot.collectAsState()
    val products = productSnapshot.products
    val archivedProducts by vm.archivedProducts.collectAsState()
    val orders by vm.orders.collectAsState()
    val stockMovements by vm.stockMovements.collectAsState()
    val productsLoaded = productSnapshot.loaded
    val lowStockCount = products.count { product -> product.branches.any(::isLowStock) }
    var appearance by remember(preferences) {
        mutableStateOf(
            AppAppearance(
                mode = ThemeMode.entries.firstOrNull {
                    it.name == preferences.getString("theme_mode", ThemeMode.SYSTEM.name)
                } ?: ThemeMode.SYSTEM,
                backgroundHex = preferences.getString("background_hex", "#F3F8FC") ?: "#F3F8FC",
                buttonHex = preferences.getString("button_hex", "#0284C7") ?: "#0284C7",
                secondaryHex = (preferences.getString("secondary_hex", "#E8F2FA") ?: "#E8F2FA")
                    .let { if (it.equals("#E8E1F2", ignoreCase = true)) "#E8F2FA" else it },
                textHex = preferences.getString("text_hex", "#263238") ?: "#263238",
                customTextEnabled = preferences.getBoolean("custom_text_enabled", false),
                bottomTabStyle = BottomTabStyle.entries.firstOrNull {
                    it.name == preferences.getString(
                        "bottom_tab_style",
                        BottomTabStyle.STANDARD.name
                    )
                } ?: BottomTabStyle.STANDARD,
                glassEffectEnabled = preferences.getBoolean("glass_effect_enabled", true)
            )
        )
    }
    val currentLowStockLevels = remember(products) {
        products.flatMap { product ->
            product.branches.filter(::isLowStock).map { it.id to it.stock }
        }.toMap()
    }
    var knownLowStockLevels by remember(preferences, selectedWarehouseId) {
        mutableStateOf(
            preferences.getStringSet("${warehousePreferencesPrefix}known_low_stock_levels", emptySet()).orEmpty()
                .mapNotNull { entry ->
                    val separator = entry.lastIndexOf('|')
                    if (separator <= 0) null else {
                        entry.substring(0, separator).let { id ->
                            id to entry.substring(separator + 1).toDoubleOrNull()
                        }
                    }
                }
                .mapNotNull { (id, stock) -> stock?.let { id to it } }
                .toMap()
        )
    }
    var unreadLowStockIds by remember(preferences, selectedWarehouseId) {
        mutableStateOf(
            preferences.getStringSet("${warehousePreferencesPrefix}unread_low_stock_ids", emptySet())
                .orEmpty().toSet()
        )
    }
    var fabX by remember(preferences, selectedWarehouseId) {
        mutableFloatStateOf(
            preferences.getFloat("${warehousePreferencesPrefix}add_fab_x", 1f).coerceIn(0f, 1f)
        )
    }
    var fabY by remember(preferences, selectedWarehouseId) {
        mutableFloatStateOf(
            preferences.getFloat("${warehousePreferencesPrefix}add_fab_y", 1f).coerceIn(0f, 1f)
        )
    }
    var filterFabX by remember(preferences, selectedWarehouseId) {
        mutableFloatStateOf(
            preferences.getFloat("${warehousePreferencesPrefix}filter_fab_x", 1f).coerceIn(0f, 1f)
        )
    }
    var filterFabY by remember(preferences, selectedWarehouseId) {
        mutableFloatStateOf(
            preferences.getFloat("${warehousePreferencesPrefix}filter_fab_y", 1f).coerceIn(0f, 1f)
        )
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val inventoryGridState = rememberLazyGridState()
    var inventoryRefreshing by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) } // Kho, Lịch sử, Bán hàng, Tổng quan, Cài đặt, Báo cáo
    var productFilter by remember { mutableStateOf(ProductFilter.ALL) }
    var sortField by remember(preferences, selectedWarehouseId) {
        mutableStateOf(
            SortField.entries.firstOrNull {
                it.name == preferences.getString("${warehousePreferencesPrefix}sort_field", "NAME")
            } ?: SortField.NAME
        )
    }
    var sortAscending by remember(preferences, selectedWarehouseId) {
        mutableStateOf(preferences.getBoolean("${warehousePreferencesPrefix}sort_ascending", true))
    }
    var unitFilter by remember(preferences, selectedWarehouseId) {
        mutableStateOf(
            StockUnitFilter.entries.firstOrNull {
                it.name == preferences.getString("${warehousePreferencesPrefix}unit_filter", "PIECE")
            } ?: StockUnitFilter.PIECE
        )
    }
    var recentOnly by remember { mutableStateOf(false) }
    var filterDialogVisible by remember { mutableStateOf(false) }
    var zoomedProduct by remember { mutableStateOf<Product?>(null) }
    var selectedSearchProduct by remember { mutableStateOf<Product?>(null) }
    var productEditTarget by remember { mutableStateOf<Product?>(null) }
    var scrollToInventoryProductId by remember { mutableStateOf<String?>(null) }
    var pendingProductImageUri by remember { mutableStateOf<Uri?>(null) }
    var showSharedImageDialog by remember { mutableStateOf(false) }
    var sharedProductQuery by remember { mutableStateOf("") }
    var handlingSharedImage by remember { mutableStateOf(false) }
    var sharedCropUri by remember { mutableStateOf<Uri?>(null) }
    var sharedCropProduct by remember { mutableStateOf<Product?>(null) }
    var productAddedMessage by remember { mutableStateOf<String?>(null) }
    var addWarehouseDialog by remember { mutableStateOf(false) }
    var joinWarehouseDialog by remember { mutableStateOf(false) }
    var pendingSharedWarehouseCode by remember { mutableStateOf<String?>(null) }
    var shareCodeDialog by remember { mutableStateOf<String?>(null) }
    var warehouseAction by remember { mutableStateOf<WarehouseInfo?>(null) }
    var renameWarehouseTarget by remember { mutableStateOf<WarehouseInfo?>(null) }
    var deleteWarehouseTarget by remember { mutableStateOf<WarehouseInfo?>(null) }
    var exportWarehouseTarget by remember { mutableStateOf<WarehouseInfo?>(null) }
    var accountDialogVisible by remember { mutableStateOf(false) }
    val shareCodePreferenceKey = "warehouse_share_code_${userId}_$selectedWarehouseId"
    val savedShareCodePreferenceKey = "warehouse_saved_share_code_${userId}_$selectedWarehouseId"
    var shareCodeEnabled by remember(preferences, userId, selectedWarehouseId) {
        mutableStateOf(
            preferences.getBoolean("warehouse_share_enabled_${userId}_$selectedWarehouseId", false)
        )
    }
    var shareCodeInput by remember(preferences, shareCodePreferenceKey) {
        mutableStateOf(preferences.getString(shareCodePreferenceKey, "").orEmpty())
    }
    var savedShareCode by remember(preferences, savedShareCodePreferenceKey) {
        mutableStateOf(preferences.getString(savedShareCodePreferenceKey, null))
    }
    val currentLowStockLevelsUpdated by rememberUpdatedState(currentLowStockLevels)
    val updateFabPosition by rememberUpdatedState<(Offset, Float, Float) -> Unit> { delta, width, height ->
        if (width > 0f) fabX = (fabX + delta.x / width).coerceIn(0f, 1f)
        if (height > 0f) fabY = (fabY + delta.y / height).coerceIn(0f, 1f)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun openProductFilter(filter: ProductFilter) {
        productFilter = filter
        recentOnly = false
        tab = 0
        scope.launch { drawerState.close() }
    }

    fun refreshInventory() {
        if (inventoryRefreshing) return
        scope.launch {
            inventoryRefreshing = true
            try {
                delay(80)
                inventoryGridState.animateScrollToItem(0)
                delay(620)
            } finally {
                inventoryRefreshing = false
            }
        }
    }

    BackHandler(
        enabled = !drawerState.isOpen && (
            tab != 0 ||
                productFilter != ProductFilter.ALL ||
                recentOnly ||
                inventoryGridState.firstVisibleItemIndex > 0 ||
                inventoryGridState.firstVisibleItemScrollOffset > 0
            )
    ) {
        if (tab != 0) {
            tab = 0
        }
        productFilter = ProductFilter.ALL
        recentOnly = false
        refreshInventory()
    }

    LaunchedEffect(warehouses, userId) {
        preferences.edit().putString(warehouseRegistryKey(userId), encodeWarehouses(warehouses)).apply()
    }

    LaunchedEffect(selectedWarehouseId, userId) {
        preferences.edit().putString(warehouseSelectionKey(userId), selectedWarehouseId).apply()
    }

    LaunchedEffect(selectedWarehouseId, userId, accountKey, localOnly) {
        if (!localOnly && accountKey != "local-device") {
            if (selectedWarehouse.cloudId != remoteWarehouseId) {
                warehouses = warehouses.map { warehouse ->
                    if (warehouse.id == selectedWarehouseId) {
                        warehouse.copy(cloudId = remoteWarehouseId)
                    } else {
                        warehouse
                    }
                }
            }
        }
    }

    LaunchedEffect(appearance) {
        preferences.edit()
            .putString("theme_mode", appearance.mode.name)
            .putString("background_hex", appearance.backgroundHex)
            .putString("button_hex", appearance.buttonHex)
            .putString("secondary_hex", appearance.secondaryHex)
            .putString("text_hex", appearance.textHex)
            .putBoolean("custom_text_enabled", appearance.customTextEnabled)
            .putString("bottom_tab_style", appearance.bottomTabStyle.name)
            .putBoolean("glass_effect_enabled", appearance.glassEffectEnabled)
            .apply()
    }

    LaunchedEffect(sortField, sortAscending, unitFilter, warehousePreferencesPrefix) {
        preferences.edit()
            .putString("${warehousePreferencesPrefix}sort_field", sortField.name)
            .putBoolean("${warehousePreferencesPrefix}sort_ascending", sortAscending)
            .putString("${warehousePreferencesPrefix}unit_filter", unitFilter.name)
            .apply()
    }

    LaunchedEffect(productsLoaded, currentLowStockLevels) {
        if (productsLoaded) {
            val active = currentLowStockLevels
            val changedSinceRead = active.filter { (id, stock) ->
                val previous = knownLowStockLevels[id]
                previous == null || stock < previous
            }.keys
            val unread = (unreadLowStockIds + changedSinceRead) intersect active.keys
            knownLowStockLevels = active
            unreadLowStockIds = unread
            preferences.edit()
                .putStringSet(
                    "${warehousePreferencesPrefix}known_low_stock_levels",
                    active.map { (id, stock) -> "$id|$stock" }.toSet()
                )
                .putStringSet("${warehousePreferencesPrefix}unread_low_stock_ids", unread)
                .apply()
        }
    }

    LaunchedEffect(fabX, fabY) {
        preferences.edit()
            .putFloat("${warehousePreferencesPrefix}add_fab_x", fabX)
            .putFloat("${warehousePreferencesPrefix}add_fab_y", fabY)
            .apply()
    }
    LaunchedEffect(filterFabX, filterFabY) {
        preferences.edit()
            .putFloat("${warehousePreferencesPrefix}filter_fab_x", filterFabX)
            .putFloat("${warehousePreferencesPrefix}filter_fab_y", filterFabY)
            .apply()
    }

    LaunchedEffect(vm.message) {
        val message = vm.message
        if (message != null) {
            vm.dismissMessage()
            if (message == "Đã thêm sản phẩm.") {
                productAddedMessage = message
            } else {
                snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = "Đã biết",
                    withDismissAction = true,
                    duration = androidx.compose.material3.SnackbarDuration.Long
                )
            }
        }
    }

    LaunchedEffect(productAddedMessage) {
        if (productAddedMessage != null) {
            delay(2_000)
            productAddedMessage = null
        }
    }

    LaunchedEffect(sharedImageUri) {
        if (sharedImageUri != null) {
            showSharedImageDialog = true
            sharedProductQuery = ""
        }
    }
    LaunchedEffect(sharedText) {
        val text = sharedText ?: return@LaunchedEffect
        val code = Regex("(?i)(?<![0-9A-F])[0-9A-F]{32}(?![0-9A-F])")
            .find(text)
            ?.value
            ?.uppercase()
        if (code == null) {
            vm.message = "Nội dung nhận được không có mã kho hợp lệ."
        } else {
            pendingSharedWarehouseCode = code
            joinWarehouseDialog = true
        }
        onSharedTextHandled()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.getSharedPreferences("kho-glass-permissions", Context.MODE_PRIVATE)
                .getBoolean("notification_requested", false).not()
        ) {
            context.getSharedPreferences("kho-glass-permissions", Context.MODE_PRIVATE)
                .edit().putBoolean("notification_requested", true).apply()
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    KhoTheme(appearance) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        Modifier.fillMaxHeight().verticalScroll(rememberScrollState())
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clickable { accountDialogVisible = true }
                                .padding(horizontal = 16.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            UserAvatar(visibleAccountPhoto, Modifier.size(48.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    accountDisplayName ?: if (localOnly) {
                                        "Đăng nhập Google"
                                    } else {
                                        "Tài khoản"
                                    },
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    accountEmail ?: if (localOnly) {
                                        "Nhấn để đăng nhập"
                                    } else {
                                        "Quản lý tài khoản"
                                    },
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Icon(Icons.Default.AccountCircle, contentDescription = "Tài khoản")
                        }
                        HorizontalDivider()
                        Text(
                            appName,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 24.dp),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        NavigationDrawerItem(
                            label = { Text("Thêm sản phẩm") },
                            selected = false,
                            icon = { Icon(Icons.Default.Add, null) },
                            onClick = {
                                vm.addDialog = true
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Tìm kiếm") },
                            selected = tab == 6,
                            icon = { Icon(Icons.Default.Search, null) },
                            onClick = {
                                tab = 6
                                productFilter = ProductFilter.ALL
                                recentOnly = false
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Quản lý kho") },
                            selected = tab == 7,
                            icon = { Icon(Icons.Default.Warehouse, null) },
                            onClick = {
                                tab = 7
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Tổng quan") },
                            selected = tab == 3,
                            icon = { Icon(Icons.Default.Home, null) },
                            onClick = {
                                tab = 3
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Sản phẩm gần đây") },
                            selected = tab == 8,
                            icon = { Icon(Icons.Default.History, null) },
                            onClick = {
                                productFilter = ProductFilter.ALL
                                recentOnly = true
                                tab = 8
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Tất cả sản phẩm") },
                            selected = productFilter == ProductFilter.ALL && tab == 0,
                            icon = { Icon(Icons.Default.Inventory2, null) },
                            onClick = { openProductFilter(ProductFilter.ALL) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Sắp hết: $lowStockCount") },
                            selected = productFilter == ProductFilter.LOW_STOCK && tab == 0,
                            icon = { Icon(Icons.Default.WarningAmber, null) },
                            onClick = { openProductFilter(ProductFilter.LOW_STOCK) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Đã ẩn") },
                            selected = productFilter == ProductFilter.ARCHIVED && tab == 0,
                            icon = { Icon(Icons.Default.VisibilityOff, null) },
                            onClick = { openProductFilter(ProductFilter.ARCHIVED) }
                        )
                        NavigationDrawerItem(
                            label = { Text("Lịch sử xuất nhập") },
                            selected = tab == 1,
                            icon = { Icon(Icons.Default.ReceiptLong, null) },
                            onClick = {
                                tab = 1
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Bán hàng") },
                            selected = tab == 2,
                            icon = { Icon(Icons.Default.ShoppingCart, null) },
                            onClick = {
                                tab = 2
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Thông báo & báo cáo") },
                            selected = tab == 5,
                            icon = { Icon(Icons.Default.Notifications, null) },
                            onClick = {
                                tab = 5
                                scope.launch { drawerState.close() }
                            }
                        )
                        NavigationDrawerItem(
                            label = { Text("Cài đặt ứng dụng") },
                            selected = tab == 4,
                            icon = { Icon(Icons.Default.Settings, null) },
                            onClick = {
                                tab = 4
                                scope.launch { drawerState.close() }
                            }
                        )
                    }
                }
            }
        ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    modifier = Modifier.height(72.dp),
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = "Mở menu",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    },
                    title = {
                        Text(
                            if (tab == 6) "Tìm kiếm" else appName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .pointerInput(tab) {
                                    detectTapGestures(
                                        onTap = {
                                            tab = 6
                                            productFilter = ProductFilter.ALL
                                            recentOnly = false
                                        },
                                        onLongPress = {
                                            if (tab == 0 || tab == 8) filterDialogVisible = true
                                            else {
                                                tab = 6
                                                productFilter = ProductFilter.ALL
                                                recentOnly = false
                                            }
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Tìm sản phẩm",
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        IconButton(
                            onClick = { tab = 5 },
                            modifier = Modifier.size(64.dp)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadLowStockIds.isNotEmpty()) {
                                        Badge { Text(unreadLowStockIds.size.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = "Thông báo và báo cáo",
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    },
                    colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(
                            alpha = if (appearance.glassEffectEnabled) 0.82f else 1f
                        )
                    )
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                val items = listOf(
                    "Kho" to Icons.Default.Inventory2,
                    "Lịch sử" to Icons.Default.ReceiptLong,
                    "Bán hàng" to Icons.Default.ShoppingCart
                )
                val selectTab: (Int) -> Unit = { index ->
                    tab = index
                    if (index == 0) {
                        productFilter = ProductFilter.ALL
                        recentOnly = false
                        scope.launch { inventoryGridState.animateScrollToItem(0) }
                    }
                }
                Column(Modifier.navigationBarsPadding().padding(bottom = 6.dp)) {
                if (appearance.bottomTabStyle == BottomTabStyle.ROUNDED) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 6.dp)
                            .height(56.dp),
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.surface.copy(
                            alpha = if (appearance.glassEffectEnabled) 0.78f else 1f
                        ),
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            items.forEachIndexed { index, (title, icon) ->
                                val selected = tab == index || (index == 0 && tab == 8)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(56.dp)
                                        .clickable { selectTab(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(RoundedCornerShape(percent = 50))
                                            .background(
                                                if (selected) {
                                                    MaterialTheme.colorScheme.secondaryContainer.copy(
                                                        alpha = if (appearance.glassEffectEnabled) {
                                                            0.88f
                                                        } else {
                                                            1f
                                                        }
                                                    )
                                                } else {
                                                    Color.Transparent
                                                }
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            icon,
                                            contentDescription = title,
                                            tint = if (selected) {
                                                MaterialTheme.colorScheme.onSecondaryContainer
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    NavigationBar(
                        modifier = Modifier.height(60.dp),
                        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp
                    ) {
                        items.forEachIndexed { index, (title, icon) ->
                            NavigationBarItem(
                                selected = tab == index || (index == 0 && tab == 8),
                                onClick = { selectTab(index) },
                                icon = { Icon(icon, contentDescription = title) },
                                colors = androidx.compose.material3.NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer.copy(
                                        alpha = if (appearance.glassEffectEnabled) 0.22f else 0.14f
                                    )
                                )
                            )
                        }
                    }
                    }
                }
            },
            floatingActionButton = {}
        ) { padding ->
            BoxWithConstraints(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize()
                    .pointerInput(tab) {
                        val edgeSize = 32.dp.toPx()
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            if (down.position.x <= edgeSize) return@awaitEachGesture
                            var totalX = 0f
                            var totalY = 0f
                            var horizontalDrag = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                                if (!change.pressed) break
                                val delta = change.positionChange()
                                totalX += delta.x
                                totalY += delta.y
                                if (!horizontalDrag) {
                                    if (kotlin.math.abs(totalY) > touchSlop) break
                                    horizontalDrag =
                                        kotlin.math.abs(totalX) > touchSlop &&
                                            kotlin.math.abs(totalX) > kotlin.math.abs(totalY)
                                }
                                if (horizontalDrag) change.consume()
                            }
                            if (horizontalDrag && kotlin.math.abs(totalX) > 80.dp.toPx()) {
                                tab = if (tab == 6) {
                                    if (totalX > 0) 0 else 1
                                } else if (tab in 0..2) {
                                    (tab + if (totalX < 0) 1 else -1).coerceIn(0, 2)
                                } else tab
                            }
                        }
                    }
            ) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = {
                        if (targetState >= initialState) {
                            (slideInHorizontally(tween(220)) { width -> width / 5 } + fadeIn(tween(180))) togetherWith
                                (slideOutHorizontally(tween(220)) { width -> -width / 6 } + fadeOut(tween(160)))
                        } else {
                            (slideInHorizontally(tween(220)) { width -> -width / 5 } + fadeIn(tween(180))) togetherWith
                                (slideOutHorizontally(tween(220)) { width -> width / 6 } + fadeOut(tween(160)))
                        }
                    },
                    label = "inventory-tab-transition"
                ) { screenTab ->
                when (screenTab) {
                    0 -> Inventory(
                        products,
                        archivedProducts,
                        vm,
                        recentOnly = false,
                        productFilter = productFilter,
                        unitFilter = unitFilter,
                        sortField = sortField,
                        sortAscending = sortAscending,
                        gridState = inventoryGridState,
                        isRefreshing = inventoryRefreshing,
                        onRefresh = ::refreshInventory,
                        scrollToProductId = scrollToInventoryProductId,
                        onProductScrolled = { scrollToInventoryProductId = null }
                    )
                    1 -> StockHistoryScreen(stockMovements)
                    2 -> SalesScreen(products, orders, vm)
                    3 -> Dashboard(products, orders)
                    4 -> SettingsScreen(
                        appName = appName,
                        canCreateShareCode = !localOnly &&
                            accountKey != "local-device" &&
                            selectedWarehouse.cloudId == null,
                        shareCodeEnabled = shareCodeEnabled,
                        shareCode = shareCodeInput,
                        savedShareCode = savedShareCode,
                        onShareCodeInputChange = { code ->
                            shareCodeInput = code
                            preferences.edit().putString(shareCodePreferenceKey, code).apply()
                        },
                        onShareCodeEnabledChange = { enabled ->
                            if (enabled && localOnly && onEnableCodeSync == null) {
                                vm.message = "Chưa cấu hình Firebase để bật đồng bộ qua mã."
                            } else if (enabled) {
                                preferences.edit()
                                    .putBoolean(
                                        "warehouse_share_enabled_${userId}_$selectedWarehouseId",
                                        true
                                    )
                                    .apply()
                                shareCodeEnabled = true
                                if (localOnly) onEnableCodeSync?.invoke()
                            } else if (localOnly) {
                                preferences.edit()
                                    .putBoolean(
                                        "warehouse_share_enabled_${userId}_$selectedWarehouseId",
                                        false
                                    )
                                    .apply()
                                shareCodeEnabled = false
                            } else {
                                scope.launch {
                                    try {
                                        com.kho.glass.data.FirestoreWarehouseSync.disableShareCode(
                                            remoteWarehouseId,
                                            accountKey
                                        )
                                        preferences.edit()
                                            .putBoolean(
                                                "warehouse_share_enabled_${userId}_$selectedWarehouseId",
                                                false
                                            )
                                            .apply()
                                        shareCodeEnabled = false
                                    } catch (error: kotlinx.coroutines.CancellationException) {
                                        throw error
                                    } catch (error: com.google.firebase.FirebaseException) {
                                        vm.message = "Không tắt được mã chia sẻ: " +
                                            (error.localizedMessage ?: "lỗi Firebase")
                                    } catch (error: IllegalStateException) {
                                        vm.message = error.message ?: "Không tắt được mã chia sẻ."
                                    }
                                }
                            }
                        },
                        onCreateShareCode = if (!localOnly && accountKey != "local-device") { code ->
                            com.kho.glass.data.FirestoreWarehouseSync.createShareCode(
                                remoteWarehouseId,
                                accountKey,
                                code
                            )
                        } else null,
                        onShareCodeCreated = { code ->
                            shareCodeInput = code
                            savedShareCode = code
                            preferences.edit()
                                .putString(shareCodePreferenceKey, code)
                                .putString(savedShareCodePreferenceKey, code)
                                .apply()
                            shareCodeDialog = code
                        },
                        onLoadShareRequests = if (!localOnly && accountKey != "local-device") ({
                            com.kho.glass.data.FirestoreWarehouseSync.pendingShareRequests(
                                remoteWarehouseId,
                                accountKey
                            )
                        }) else null,
                        onDecideShareRequest = if (!localOnly && accountKey != "local-device") { memberUid, approve ->
                            com.kho.glass.data.FirestoreWarehouseSync.decideShareRequest(
                                remoteWarehouseId,
                                accountKey,
                                memberUid,
                                approve
                            )
                        } else null,
                        onReceiveNearbyShareCode = { joinWarehouseDialog = true },
                        accountDisplayName = accountDisplayName,
                        accountEmail = accountEmail,
                        accountPhoto = visibleAccountPhoto,
                        localOnly = localOnly,
                        syncError = syncError.takeUnless { localOnly },
                        onRetrySync = {
                            com.kho.glass.data.FirestoreWarehouseSync.schedule(
                                context,
                                accountKey,
                                userId,
                                selectedWarehouseId,
                                remoteWarehouseId
                            )
                        },
                        canChangeAccount = onRequestGoogleSignIn != null,
                        onRequestGoogleSignIn = onRequestGoogleSignIn,
                        onEnableCodeSync = onEnableCodeSync,
                        onSignOut = onSignOut,
                        onPhotoChange = { photo ->
                            customAccountPhoto = photo
                            preferences.edit()
                                .putString("account_photo_$accountKey", photo)
                                .apply()
                        },
                        appearance = appearance,
                        products = products,
                        onAppNameChange = { name ->
                            warehouses = warehouses.map { warehouse ->
                                if (warehouse.id == selectedWarehouseId) {
                                    warehouse.copy(name = name)
                                } else warehouse
                            }
                        },
                        onAppearanceChange = { appearance = it },
                        onBackupMessage = { vm.message = it },
                        onImport = vm::importInventory,
                        onDeleteAllProducts = vm::permanentlyDeleteAllProducts
                    )
                    6 -> SearchScreen(
                        products = products,
                        vm = vm,
                        unitFilter = unitFilter,
                        sortField = sortField,
                        sortAscending = sortAscending,
                        onImageClick = { zoomedProduct = it },
                        onProductDetails = { selectedSearchProduct = it },
                        onProductEdit = {
                            selectedSearchProduct = null
                            productEditTarget = it
                            tab = 0
                        }
                    )
                    8 -> Inventory(
                        products,
                        archivedProducts,
                        vm,
                        recentOnly = true,
                        productFilter = ProductFilter.ALL,
                        unitFilter = StockUnitFilter.ALL,
                        sortField = SortField.STOCK,
                        sortAscending = false,
                        gridState = inventoryGridState,
                        isRefreshing = inventoryRefreshing,
                        onRefresh = ::refreshInventory
                    )
                    7 -> WarehouseManagerScreen(
                        warehouses = warehouses,
                        activeWarehouseId = selectedWarehouseId,
                        onSelect = { warehouse ->
                            selectedWarehouseId = warehouse.id
                            productFilter = ProductFilter.ALL
                            recentOnly = false
                            vm.query = ""
                            tab = 0
                        },
                        onAdd = { addWarehouseDialog = true },
                        onJoin = { joinWarehouseDialog = true },
                        onManage = { warehouseAction = it }
                    )
                    else -> Reports(
                        products,
                        orders,
                        unreadLowStockIds.size,
                        onMarkLowStockRead = {
                            val current = currentLowStockLevelsUpdated
                            knownLowStockLevels = current
                            unreadLowStockIds = emptySet()
                            preferences.edit()
                                .putStringSet(
                                    "${warehousePreferencesPrefix}known_low_stock_levels",
                                    current.map { (id, stock) -> "$id|$stock" }.toSet()
                                )
                                .putStringSet(
                                    "${warehousePreferencesPrefix}unread_low_stock_ids",
                                    emptySet()
                                )
                                .apply()
                        }
                    )
                }
                }
                productAddedMessage?.let { message ->
                    ProductAddedBanner(
                        message = message,
                        onDismiss = { productAddedMessage = null },
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp)
                    )
                }
                if (tab == 0 || tab == 8) {
                    val fabSize = 56.dp
                    val fabMargin = 16.dp
                    val density = LocalDensity.current
                    FloatingActionButton(
                        onClick = { vm.addDialog = true },
                        modifier = Modifier
                            .offset(
                                x = fabMargin +
                                    (maxWidth - fabSize - fabMargin * 2).coerceAtLeast(0.dp) * fabX,
                                y = fabMargin +
                                    (maxHeight - fabSize - fabMargin * 2).coerceAtLeast(0.dp) * fabY
                            )
                            .size(fabSize)
                            .pointerInput(maxWidth, maxHeight) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    val downAt = SystemClock.uptimeMillis()
                                    var dragging = false
                                    var finished = false
                                    while (!finished) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.firstOrNull {
                                            it.id == down.id
                                        }
                                        if (change == null) {
                                            finished = true
                                        } else if (!change.pressed) {
                                            if (!dragging &&
                                                SystemClock.uptimeMillis() - downAt < 1_000L
                                            ) {
                                                vm.addDialog = true
                                            }
                                            finished = true
                                        } else if (
                                            SystemClock.uptimeMillis() - downAt >= 1_000L
                                        ) {
                                            dragging = true
                                            val availableWidth =
                                                with(density) {
                                                    (maxWidth - fabSize - fabMargin * 2)
                                                        .coerceAtLeast(0.dp).toPx()
                                                }
                                            val availableHeight =
                                                with(density) {
                                                    (maxHeight - fabSize - fabMargin * 2)
                                                        .coerceAtLeast(0.dp).toPx()
                                                }
                                            updateFabPosition(
                                                change.positionChange(),
                                                availableWidth,
                                                availableHeight
                                            )
                                            change.consume()
                                        }
                                    }
                                }
                            },
                        containerColor = MaterialTheme.colorScheme.primary.copy(
                            alpha = if (appearance.glassEffectEnabled) 0.84f else 1f
                        ),
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Nhấn giữ 1 giây để di chuyển; chạm để thêm sản phẩm",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } else if (tab == 6) {
                    val fabSize = 56.dp
                    val fabMargin = 16.dp
                    val density = LocalDensity.current
                    Box(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        FloatingActionButton(
                            onClick = { filterDialogVisible = true },
                            containerColor = MaterialTheme.colorScheme.primary.copy(
                                alpha = if (appearance.glassEffectEnabled) 0.84f else 1f
                            ),
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .offset(
                                    x = fabMargin +
                                        (maxWidth - fabSize - fabMargin * 2).coerceAtLeast(0.dp) *
                                        filterFabX,
                                    y = fabMargin +
                                        (maxHeight - fabSize - fabMargin * 2).coerceAtLeast(0.dp) *
                                        filterFabY
                                )
                                .size(fabSize)
                                .pointerInput(maxWidth, maxHeight) {
                                    awaitEachGesture {
                                        val down = awaitFirstDown(requireUnconsumed = false)
                                        val downAt = SystemClock.uptimeMillis()
                                        var dragging = false
                                        var finished = false
                                        while (!finished) {
                                            val event = awaitPointerEvent()
                                            val change = event.changes.firstOrNull {
                                                it.id == down.id
                                            }
                                            if (change == null) {
                                                finished = true
                                            } else if (!change.pressed) {
                                                if (!dragging &&
                                                    SystemClock.uptimeMillis() - downAt < 1_000L
                                                ) {
                                                    filterDialogVisible = true
                                                }
                                                finished = true
                                            } else if (
                                                SystemClock.uptimeMillis() - downAt >= 1_000L
                                            ) {
                                                dragging = true
                                                val availableWidth = with(density) {
                                                    (maxWidth - fabSize - fabMargin * 2)
                                                        .coerceAtLeast(0.dp).toPx()
                                                }
                                                val availableHeight = with(density) {
                                                    (maxHeight - fabSize - fabMargin * 2)
                                                        .coerceAtLeast(0.dp).toPx()
                                                }
                                                if (availableWidth > 0f) {
                                                    filterFabX = (
                                                        filterFabX +
                                                            change.positionChange().x / availableWidth
                                                        ).coerceIn(0f, 1f)
                                                }
                                                if (availableHeight > 0f) {
                                                    filterFabY = (
                                                        filterFabY +
                                                            change.positionChange().y / availableHeight
                                                        ).coerceIn(0f, 1f)
                                                }
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Nhấn giữ 1 giây để di chuyển; chạm để lọc"
                            )
                        }
                    }
                }
            }
        }

        if (vm.addDialog) {
            ProductDialog(
                vm = vm,
                product = null,
                initialImageUri = pendingProductImageUri,
                onDismiss = {
                    vm.addDialog = false
                    pendingProductImageUri = null
                }
            )
        }
        if (accountDialogVisible) {
            AccountChooserDialog(
                displayName = accountDisplayName,
                email = accountEmail,
                photo = visibleAccountPhoto,
                localOnly = localOnly,
                canSignIn = onRequestGoogleSignIn != null,
                onDismiss = { accountDialogVisible = false },
                onAddAccount = {
                    accountDialogVisible = false
                    onRequestGoogleSignIn?.invoke()
                },
                onEnableCodeSync = onEnableCodeSync?.let { enableSync ->
                    {
                        accountDialogVisible = false
                        enableSync()
                    }
                },
                onSignOut = onSignOut?.let { signOut ->
                    {
                        accountDialogVisible = false
                        signOut()
                    }
                }
            )
        }
        zoomedProduct?.let { product ->
            ProductImageViewer(product = product, onDismiss = { zoomedProduct = null })
        }
        selectedSearchProduct?.let { product ->
            ProductQuickDetailsDialog(
                product = product,
                onDismiss = { selectedSearchProduct = null },
                onEdit = {
                    selectedSearchProduct = null
                    productEditTarget = product
                    tab = 0
                },
                onViewInInventory = {
                    selectedSearchProduct = null
                    productFilter = ProductFilter.ALL
                    recentOnly = false
                    unitFilter = StockUnitFilter.ALL
                    scrollToInventoryProductId = product.id
                    tab = 0
                },
                onHide = {
                    vm.deactivateProduct(product.id)
                    selectedSearchProduct = null
                }
            )
        }
        productEditTarget?.let { product ->
            ProductDialog(
                vm = vm,
                product = product,
                onDismiss = { productEditTarget = null }
            )
        }
        if (filterDialogVisible) {
            ProductFilterDialog(
                unitFilter = unitFilter,
                sortField = sortField,
                sortAscending = sortAscending,
                onUnitFilterChange = { unitFilter = it },
                onSortFieldChange = { sortField = it },
                onSortAscendingChange = { sortAscending = it },
                onDismiss = { filterDialogVisible = false }
            )
        }
        if (showSharedImageDialog && sharedImageUri != null) {
            SharedImageDialog(
                products = products,
                query = sharedProductQuery,
                working = handlingSharedImage,
                onQueryChange = { sharedProductQuery = it },
                onCreateNew = {
                    pendingProductImageUri = sharedImageUri
                    vm.addDialog = true
                    showSharedImageDialog = false
                    onSharedImageHandled()
                },
                onSelectProduct = { product ->
                    sharedCropUri = sharedImageUri
                    sharedCropProduct = product
                    showSharedImageDialog = false
                    onSharedImageHandled()
                },
                onDismiss = {
                    showSharedImageDialog = false
                    onSharedImageHandled()
                }
            )
        }
        sharedCropUri?.let { uri ->
            PhotoCropDialog(
                sourceUri = uri,
                sourceBitmap = null,
                onDismiss = {
                    sharedCropUri = null
                    sharedCropProduct = null
                },
                onCropComplete = { bitmap ->
                    val target = sharedCropProduct
                    sharedCropUri = null
                    sharedCropProduct = null
                    if (target != null) {
                        scope.launch {
                            handlingSharedImage = true
                            try {
                                val imagePath = withContext(Dispatchers.IO) {
                                    persistProductImage(
                                        context,
                                        null,
                                        bitmap,
                                        queryImageSize(context, uri)
                                    )
                                }
                                vm.updateProduct(target.id, target.name, imagePath)
                            } catch (failure: IOException) {
                                vm.message =
                                    "Không lưu được ảnh chia sẻ: ${failure.localizedMessage ?: "lỗi tệp"}"
                            } finally {
                                handlingSharedImage = false
                            }
                        }
                    }
                },
                onCropError = { message ->
                    vm.message = "Không cắt được ảnh chia sẻ: $message"
                }
            )
        }
        if (addWarehouseDialog) {
            WarehouseNameDialog(
                title = "Thêm kho",
                initialName = "",
                confirmText = "Tạo kho",
                onDismiss = { addWarehouseDialog = false },
                onSave = { name ->
                    val warehouse = WarehouseInfo(UUID.randomUUID().toString(), name)
                    warehouses = warehouses + warehouse
                    selectedWarehouseId = warehouse.id
                    productFilter = ProductFilter.ALL
                    recentOnly = false
                    vm.query = ""
                    tab = 0
                    addWarehouseDialog = false
                    vm.message = "Đã tạo kho mới."
                }
            )
        }
        if (joinWarehouseDialog) {
            WarehouseCodeDialog(
                initialCode = pendingSharedWarehouseCode.orEmpty(),
                onDismiss = {
                    joinWarehouseDialog = false
                    pendingSharedWarehouseCode = null
                },
                onJoin = { code ->
                    joinWarehouseDialog = false
                    pendingSharedWarehouseCode = null
                    if (localOnly || accountKey == "local-device") {
                        vm.message = "Hãy bật đồng bộ qua mã trước khi nhập mã kho."
                    } else {
                        scope.launch {
                            try {
                                val cloudId =
                                    com.kho.glass.data.FirestoreWarehouseSync.joinWithShareCode(
                                        code,
                                        accountKey
                                    )
                                val warehouse = WarehouseInfo(
                                    id = UUID.randomUUID().toString(),
                                    name = "Kho được chia sẻ",
                                    cloudId = cloudId
                                )
                                warehouses = warehouses + warehouse
                                selectedWarehouseId = warehouse.id
                                productFilter = ProductFilter.ALL
                                recentOnly = false
                                vm.query = ""
                                tab = 0
                                vm.message =
                                    "Đã gửi yêu cầu tham gia. Chờ chủ kho duyệt trước khi đồng bộ dữ liệu."
                            } catch (error: kotlinx.coroutines.CancellationException) {
                                throw error
                            } catch (error: com.google.firebase.FirebaseException) {
                                vm.message = "Không kết nối được kho: " +
                                    (error.localizedMessage ?: "lỗi Firebase")
                            } catch (error: IllegalArgumentException) {
                                vm.message = error.message ?: "Mã kho không hợp lệ."
                            } catch (error: IllegalStateException) {
                                vm.message = error.message ?: "Không thể kết nối kho."
                            }
                        }
                    }
                }
            )
        }
        shareCodeDialog?.let { code ->
            AlertDialog(
                onDismissRequest = { shareCodeDialog = null },
                title = { DialogTitleWithClose("Mã chia sẻ kho") { shareCodeDialog = null } },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chia sẻ mã này với người cần truy cập. Mỗi mã mới sẽ thay mã cũ.")
                        Text(code, style = MaterialTheme.typography.titleMedium)
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE)
                            as android.content.ClipboardManager
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText("Mã kho", code)
                        )
                        shareCodeDialog = null
                        vm.message = "Đã sao chép mã kho."
                    }) { Text("Sao chép") }
                },
                dismissButton = {
                    TextButton(onClick = { shareCodeDialog = null }) { Text("Đóng") }
                }
            )
        }
        renameWarehouseTarget?.let { warehouse ->
            WarehouseNameDialog(
                title = "Đổi tên kho",
                initialName = warehouse.name,
                confirmText = "Lưu",
                onDismiss = { renameWarehouseTarget = null },
                onSave = { name ->
                    warehouses = warehouses.map {
                        if (it.id == warehouse.id) it.copy(name = name) else it
                    }
                    renameWarehouseTarget = null
                }
            )
        }
        warehouseAction?.let { warehouse ->
            AlertDialog(
                onDismissRequest = { warehouseAction = null },
                title = { DialogTitleWithClose(warehouse.name) { warehouseAction = null } },
                text = {
                    Column {
                        Text("Chọn thao tác cho kho này.")
                        if (!localOnly && accountKey != "local-device") {
                            TextButton(onClick = {
                                warehouseAction = null
                                val remoteId = warehouse.cloudId
                                    ?: UUID.nameUUIDFromBytes(
                                        "$accountKey|${warehouse.id}".toByteArray()
                                    ).toString()
                                scope.launch {
                                    try {
                                        shareCodeDialog =
                                            com.kho.glass.data.FirestoreWarehouseSync.createShareCode(
                                                remoteId,
                                                accountKey
                                            )
                                    } catch (error: kotlinx.coroutines.CancellationException) {
                                        throw error
                                    } catch (error: com.google.firebase.FirebaseException) {
                                        vm.message = "Không tạo được mã chia sẻ: " +
                                            (error.localizedMessage ?: "lỗi Firebase")
                                    } catch (error: IllegalStateException) {
                                        vm.message = error.message ?: "Không thể tạo mã chia sẻ."
                                    }
                                }
                            }) { Text("Tạo hoặc đổi mã chia sẻ") }
                        }
                        TextButton(onClick = {
                            warehouseAction = null
                            renameWarehouseTarget = warehouse
                        }) { Text("Đổi tên") }
                        TextButton(onClick = {
                            warehouseAction = null
                            exportWarehouseTarget = warehouse
                        }) { Text("Xuất ZIP sản phẩm") }
                        TextButton(
                            enabled = warehouses.size > 1,
                            onClick = {
                                warehouseAction = null
                                deleteWarehouseTarget = warehouse
                            }
                        ) { Text("Xóa kho") }
                        if (warehouses.size == 1) {
                            Text(
                                "Cần giữ lại ít nhất một kho.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { warehouseAction = null }) { Text("Đóng") }
                }
            )
        }
        deleteWarehouseTarget?.let { warehouse ->
            AlertDialog(
                onDismissRequest = { deleteWarehouseTarget = null },
                title = { DialogTitleWithClose("Xóa kho “${warehouse.name}”?") { deleteWarehouseTarget = null } },
                text = {
                    Text("Sản phẩm, đơn hàng, lịch sử và dữ liệu trong kho này sẽ bị xóa khỏi thiết bị. Không thể hoàn tác.")
                },
                confirmButton = {
                    TextButton(onClick = {
                        val remaining = warehouses.filterNot { it.id == warehouse.id }
                        scope.launch {
                            try {
                                val undeletedImages = withContext(Dispatchers.IO) {
                                    LocalStore.deleteWarehouseDatabase(context, userId, warehouse.id)
                                }
                                warehouses = remaining
                                if (selectedWarehouseId == warehouse.id) {
                                    selectedWarehouseId = remaining.first().id
                                    tab = 0
                                    productFilter = ProductFilter.ALL
                                    recentOnly = false
                                    vm.query = ""
                                }
                                deleteWarehouseTarget = null
                                vm.message = if (undeletedImages.isEmpty()) {
                                    "Đã xóa kho “${warehouse.name}”."
                                } else {
                                    "Đã xóa kho; không xóa được ${undeletedImages.size} ảnh."
                                }
                            } catch (failure: IllegalStateException) {
                                vm.message = failure.localizedMessage ?: "Không xóa được dữ liệu kho."
                            } catch (failure: IOException) {
                                vm.message = failure.localizedMessage ?: "Không xóa được ảnh trong kho."
                            } catch (failure: SQLiteException) {
                                vm.message = failure.localizedMessage ?: "Không đọc được dữ liệu kho."
                            } catch (failure: SecurityException) {
                                vm.message = failure.localizedMessage ?: "Ứng dụng không được phép xóa dữ liệu kho."
                            }
                        }
                    }) { Text("Xóa kho") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteWarehouseTarget = null }) { Text("Hủy") }
                }
            )
        }
        val warehouseExportLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.CreateDocument("application/zip")
        ) { uri ->
            val warehouse = exportWarehouseTarget
            exportWarehouseTarget = null
            if (uri != null && warehouse != null) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            val warehouseStore = LocalStore(context, userId, warehouse.id)
                            try {
                                InventoryBackup.write(
                                    context,
                                    uri,
                                    warehouseStore.exportProducts()
                                )
                            } finally {
                                warehouseStore.close()
                            }
                        }
                        vm.message = "Đã xuất sản phẩm từ kho “${warehouse.name}”."
                    } catch (failure: IOException) {
                        vm.message = "Không xuất được ZIP: ${failure.localizedMessage ?: "lỗi tệp"}"
                    } catch (failure: IllegalStateException) {
                        vm.message = failure.localizedMessage ?: "Không thể đọc dữ liệu kho."
                    }
                }
            }
        }
        LaunchedEffect(exportWarehouseTarget) {
            exportWarehouseTarget?.let { warehouse ->
                val safeName = warehouse.name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
                warehouseExportLauncher.launch("QL-kho-$safeName.zip")
            }
        }
        }
    }
}

@Composable
private fun Dashboard(products: List<Product>, orders: List<SalesOrder>) {
    val stockValue = products.sumOf { product ->
        product.branches.sumOf { calculateAmount(it.stock, it.buyPrice) }
    }
    val activeOrders = orders.filter { it.status != "CANCELLED" }
    val exported = activeOrders.sumOf { it.totalAmount }
    val receivable = activeOrders.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0L) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Text("Tổng quan", style = MaterialTheme.typography.headlineMedium) }
        item { Metric("Giá trị tồn kho", money(stockValue)) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Metric("Giá trị đã xuất", money(exported), Modifier.weight(1f))
                Metric("Còn phải thu", money(receivable), Modifier.weight(1f))
            }
        }
        item {
            GlassCard {
                Text("Dữ liệu kho")
                Text("Đang lưu trên thiết bị để dùng offline.", color = MaterialTheme.colorScheme.primary)
            }
        }
        item { Text("Sản phẩm gần đây", style = MaterialTheme.typography.titleLarge) }
        if (products.isEmpty()) {
            item { EmptyState("Chưa có sản phẩm. Mở Kho và nhấn + để thêm.") }
        } else {
            items(products.take(5), key = { it.id }) { product ->
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(product.name, style = MaterialTheme.typography.titleMedium)
                    product.branches.forEach { branch ->
                        Text(
                            "${branch.name}: ${quantity(branch.stock, branch.unitType)}",
                            color = if (isLowStock(branch)) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WarehouseManagerScreen(
    warehouses: List<WarehouseInfo>,
    activeWarehouseId: String,
    onSelect: (WarehouseInfo) -> Unit,
    onAdd: () -> Unit,
    onJoin: () -> Unit,
    onManage: (WarehouseInfo) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Quản lý kho", style = MaterialTheme.typography.headlineSmall)
                TextButton(onClick = onJoin) { Text("Nhập mã") }
                IconButton(onClick = onAdd) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm kho")
                }
            }
        }
        items(warehouses, key = { it.id }) { warehouse ->
            val isCurrent = warehouse.id == activeWarehouseId
            GlassCard(
                Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = { if (!isCurrent) onSelect(warehouse) },
                        onLongClick = { onManage(warehouse) }
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Warehouse,
                        contentDescription = null,
                        tint = if (isCurrent) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(warehouse.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (isCurrent) "Kho hiện tại" else "Nhấn để chuyển · giữ để quản lý",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (isCurrent) {
                        Icon(Icons.Default.Check, contentDescription = "Đang chọn")
                    } else {
                        IconButton(onClick = { onManage(warehouse) }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn kho")
                        }
                    }
                }
            }
        }
        item {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Text("  Thêm kho")
            }
        }
    }
}

@Composable
private fun WarehouseCodeDialog(
    initialCode: String = "",
    onDismiss: () -> Unit,
    onJoin: (String) -> Unit
) {
    var code by remember(initialCode) { mutableStateOf(initialCode) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleWithClose("Nhập mã kho", onDismiss) },
        text = {
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mã chia sẻ") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                enabled = code.isNotBlank(),
                onClick = { onJoin(code.trim()) }
            ) { Text("Kết nối") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
private fun WarehouseNameDialog(
    title: String,
    initialName: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(initialName, title) { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleWithClose(title, onDismiss) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Tên kho") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = { onSave(name.trim()) }
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

private fun warehouseRegistryKey(userId: String): String =
    "warehouse_registry_${UUID.nameUUIDFromBytes(userId.toByteArray())}"

private fun warehouseSelectionKey(userId: String): String =
    "warehouse_selected_${UUID.nameUUIDFromBytes(userId.toByteArray())}"

private fun loadWarehouses(
    preferences: android.content.SharedPreferences,
    userId: String
): List<WarehouseInfo> {
    val saved = preferences.getString(warehouseRegistryKey(userId), null)
    val warehouses = if (saved.isNullOrBlank()) {
        emptyList()
    } else {
        try {
            val array = JSONArray(saved)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val id = item.optString("id").takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                val name = item.optString("name").trim().takeIf { it.isNotBlank() }
                    ?: return@mapNotNull null
                WarehouseInfo(
                    id = id,
                    name = name,
                    cloudId = (item.opt("cloudId") as? String)?.takeIf { it.isNotBlank() }
                )
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }
    if (!saved.isNullOrBlank() && warehouses.isNotEmpty()) return warehouses.distinctBy { it.id }
    val defaultName = preferences.getString("app_name", "Quản lý kho") ?: "Quản lý kho"
    return (listOf(WarehouseInfo(DEFAULT_WAREHOUSE_ID, defaultName)) + warehouses)
        .distinctBy { it.id }
}

private fun encodeWarehouses(warehouses: List<WarehouseInfo>): String =
    JSONArray().apply {
        warehouses.forEach { warehouse ->
            put(
                JSONObject()
                    .put("id", warehouse.id)
                    .put("name", warehouse.name)
                    .put("cloudId", warehouse.cloudId ?: JSONObject.NULL)
            )
        }
    }.toString()

@Composable
private fun StockHistoryScreen(movements: List<StockMovementView>) {
    if (movements.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            EmptyState("Chưa có lịch sử xuất nhập kho.")
        }
        return
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Lịch sử xuất nhập kho", style = MaterialTheme.typography.titleLarge)
        }
        items(movements, key = { it.id }) { movement ->
            val isIncoming = movement.quantityDelta >= 0.0
            val isLifecycle = movement.type in setOf(
                "HIDE_PRODUCT",
                "PARTIAL_HIDE",
                "RESTORE_PRODUCT",
                "DELETE_PRODUCT"
            )
            val movementIcon = when {
                movement.productDeleted && movement.type in setOf(
                    "HIDE_PRODUCT",
                    "DELETE_PRODUCT"
                ) -> Icons.Default.Close
                movement.type == "HIDE_PRODUCT" || movement.type == "PARTIAL_HIDE" ->
                    Icons.Default.VisibilityOff
                movement.type == "RESTORE_PRODUCT" -> Icons.Default.Restore
                movement.type == "DELETE_PRODUCT" -> Icons.Default.Close
                isIncoming -> Icons.Default.ArrowDownward
                else -> Icons.Default.ArrowUpward
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        movementIcon,
                        contentDescription = null,
                        tint = when {
                            movement.type == "DELETE_PRODUCT" ||
                                (movement.productDeleted && movement.type == "HIDE_PRODUCT") ->
                                MaterialTheme.colorScheme.error
                            movement.type == "HIDE_PRODUCT" || movement.type == "PARTIAL_HIDE" ->
                                MaterialTheme.colorScheme.tertiary
                            movement.type == "RESTORE_PRODUCT" ->
                                MaterialTheme.colorScheme.primary
                            isIncoming -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    Column(Modifier.weight(1f).padding(start = 10.dp)) {
                        Text(
                            movement.productName +
                                if (movement.productDeleted && !isLifecycle) " · đã xóa" else "",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "${movement.branchName} · ${stockMovementTitle(movement.type)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (movement.type == "PARTIAL_HIDE" || !isLifecycle) {
                        Text(
                            "${if (isIncoming) "+" else ""}${quantity(movement.quantityDelta)}",
                            color = if (isIncoming) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Text(
                    dateTime(movement.happenedAt),
                    modifier = Modifier.padding(start = 34.dp),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun stockMovementTitle(type: String): String = when (type) {
    "OPENING" -> "Tồn đầu kỳ"
    "RECEIVE" -> "Nhập hàng"
    "RESTORE" -> "Khôi phục tồn"
    "SALE" -> "Bán hàng"
    "CANCEL_SALE" -> "Hủy đơn, hoàn kho"
    "HIDE_PRODUCT" -> "Ẩn sản phẩm"
    "PARTIAL_HIDE" -> "Ẩn một phần tồn kho"
    "RESTORE_PRODUCT" -> "Khôi phục sản phẩm"
    "DELETE_PRODUCT" -> "Đã xóa sản phẩm"
    else -> type
}

@Composable
private fun Metric(title: String, value: String, modifier: Modifier = Modifier) {
    GlassCard(modifier) {
        Text(title)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
private fun SearchScreen(
    products: List<Product>,
    vm: KhoViewModel,
    unitFilter: StockUnitFilter,
    sortField: SortField,
    sortAscending: Boolean,
    onImageClick: (Product) -> Unit,
    onProductDetails: (Product) -> Unit,
    onProductEdit: (Product) -> Unit
) {
    val filtered = products
        .filter { vm.query.isBlank() || it.name.contains(vm.query.trim(), ignoreCase = true) }
        .filter { it.matchesUnitFilter(unitFilter) }
    val sorted = filtered.sortedWith(productComparator(sortField, sortAscending, unitFilter))
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        OutlinedTextField(
            value = vm.query,
            onValueChange = { vm.query = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Tìm tên sản phẩm") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (vm.query.isNotEmpty()) {
                    IconButton(onClick = { vm.query = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Xóa tìm kiếm")
                    }
                }
            }
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("${sorted.size} kết quả")
            Text(
                "${unitFilter.title} · ${sortField.title} ${if (sortAscending) "↑" else "↓"}",
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (sorted.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(if (vm.query.isBlank()) "Kho chưa có sản phẩm." else "Không tìm thấy sản phẩm.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sorted, key = { it.id }) { product ->
                    val stockText = product.branches
                        .filterFor(unitFilter)
                        .joinToString(" · ") { branch ->
                            "${branch.name}: ${quantity(branch.stock, branch.unitType)}"
                        }
                    val sellPriceText = product.branches
                        .filterFor(unitFilter)
                        .joinToString(" · ") { branch ->
                            "${branch.name}: Bán ${money(branch.sellPrice)}"
                        }
                    val stockColor = productStockColor(product, unitFilter)
                        .takeUnless { it == Color.Unspecified }
                        ?: MaterialTheme.colorScheme.onSurfaceVariant
                    GlassCard(Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = product.imagePath != null) {
                                        onImageClick(product)
                                    }
                            ) {
                                ProductImage(product.imagePath, Modifier.fillMaxSize())
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(
                                    product.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1
                                )
                                Text(
                                    stockText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = stockColor,
                                    maxLines = 1
                                )
                                Text(
                                    sellPriceText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { onProductEdit(product) }) {
                                Icon(Icons.Default.Edit, contentDescription = "Sửa sản phẩm")
                            }
                            IconButton(onClick = { onProductDetails(product) }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Chi tiết sản phẩm")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductFilterDialog(
    unitFilter: StockUnitFilter,
    sortField: SortField,
    sortAscending: Boolean,
    onUnitFilterChange: (StockUnitFilter) -> Unit,
    onSortFieldChange: (SortField) -> Unit,
    onSortAscendingChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleWithClose("Lọc và sắp xếp sản phẩm", onDismiss) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Loại hàng")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(StockUnitFilter.PIECE, StockUnitFilter.WEIGHT, StockUnitFilter.ALL)
                        .forEach { unit ->
                            FilterChip(
                                selected = unitFilter == unit,
                                onClick = { onUnitFilterChange(unit) },
                                label = { Text(unit.title) }
                            )
                        }
                }
                Text("Sắp xếp theo")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SortField.entries.forEach { field ->
                        FilterChip(
                            selected = sortField == field,
                            onClick = { onSortFieldChange(field) },
                            label = { Text(field.title) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = sortAscending,
                        onClick = { onSortAscendingChange(true) },
                        label = { Text("Tăng dần ↑") }
                    )
                    FilterChip(
                        selected = !sortAscending,
                        onClick = { onSortAscendingChange(false) },
                        label = { Text("Giảm dần ↓") }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Xong") }
        }
    )
}

@Composable
private fun ProductQuickDetailsDialog(
    product: Product,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onViewInInventory: () -> Unit,
    onHide: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleWithClose(product.name, onDismiss) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                product.branches.forEach { branch ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(branch.name, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Tồn ${quantity(branch.stock, branch.unitType)}",
                                color = stockStatusColor(branch)
                                    .takeUnless { it == Color.Unspecified }
                                    ?: MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "Giá bán ${money(branch.sellPrice)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onViewInInventory) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Text(" Kho")
                }
                TextButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Text(" Sửa")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onHide) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = "Ẩn sản phẩm")
                }
            }
        },
        dismissButton = null
    )
}

@Composable
private fun SharedImageDialog(
    products: List<Product>,
    query: String,
    working: Boolean,
    onQueryChange: (String) -> Unit,
    onCreateNew: () -> Unit,
    onSelectProduct: (Product) -> Unit,
    onDismiss: () -> Unit
) {
    val matches = products.filter {
        it.name.contains(query.trim(), ignoreCase = true)
    }
    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { DialogTitleWithClose("Sử dụng ảnh được chia sẻ") { if (!working) onDismiss() } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Chọn tạo sản phẩm mới hoặc cập nhật ảnh cho sản phẩm có sẵn.")
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text("Tìm sản phẩm") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
                if (working) {
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                } else if (matches.isEmpty()) {
                    Text(if (products.isEmpty()) "Kho chưa có sản phẩm." else "Không tìm thấy sản phẩm.")
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(matches, key = { it.id }) { product ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onSelectProduct(product) }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProductImage(
                                    product.imagePath,
                                    Modifier.size(44.dp).clip(RoundedCornerShape(8.dp))
                                )
                                Text(
                                    product.name,
                                    modifier = Modifier.weight(1f).padding(start = 10.dp)
                                )
                                Icon(Icons.Default.Add, contentDescription = "Dùng ảnh cho sản phẩm này")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onCreateNew, enabled = !working) {
                Text("Tạo sản phẩm mới")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !working) { Text("Hủy") }
        }
    )
}

private fun Product.matchesUnitFilter(filter: StockUnitFilter): Boolean =
    filter == StockUnitFilter.ALL || branches.any {
        when (filter) {
            StockUnitFilter.PIECE -> it.unitType == UnitType.PIECE
            StockUnitFilter.WEIGHT -> it.unitType == UnitType.WEIGHT
            StockUnitFilter.ALL -> true
        }
    }

private fun List<UnitBranch>.filterFor(filter: StockUnitFilter): List<UnitBranch> =
    when (filter) {
        StockUnitFilter.PIECE -> this.filter { it.unitType == UnitType.PIECE }
        StockUnitFilter.WEIGHT -> this.filter { it.unitType == UnitType.WEIGHT }
        StockUnitFilter.ALL -> this
    }

private fun productComparator(
    field: SortField,
    ascending: Boolean,
    unitFilter: StockUnitFilter
): Comparator<Product> {
    val comparator = when (field) {
        SortField.NAME -> compareBy<Product> { it.name.lowercase(Locale.ROOT) }
        SortField.STOCK -> compareBy<Product> {
            it.branches.filterFor(unitFilter).sumOf { branch -> branch.stock }
        }
        SortField.PRICE -> compareBy<Product> {
            it.branches.filterFor(unitFilter).minOfOrNull { branch -> branch.sellPrice }
                ?: Long.MAX_VALUE
        }
    }
    return if (ascending) comparator else comparator.reversed()
}

private fun stockStatusColor(branch: UnitBranch): Color = when {
    branch.stock <= 0.0 -> Color(0xFFE53935)
    branch.unitType == UnitType.PIECE && branch.stock <= 2.0 -> Color(0xFFFFB300)
    branch.unitType == UnitType.WEIGHT && branch.stock < 20.0 && branch.stock >= 0.1 ->
        Color(0xFFFFB300)
    else -> Color.Unspecified
}

private fun productStockColor(product: Product, filter: StockUnitFilter): Color {
    val branches = product.branches.filterFor(filter)
    return when {
        branches.any { it.stock <= 0.0 } -> Color(0xFFE53935)
        branches.any { stockStatusColor(it) == Color(0xFFFFB300) } -> Color(0xFFFFB300)
        else -> Color.Unspecified
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun Inventory(
    products: List<Product>,
    archivedProducts: List<Product>,
    vm: KhoViewModel,
    recentOnly: Boolean,
    productFilter: ProductFilter,
    unitFilter: StockUnitFilter,
    sortField: SortField,
    sortAscending: Boolean,
    gridState: LazyGridState,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    scrollToProductId: String? = null,
    onProductScrolled: () -> Unit = {}
) {
    var receivingBranch by remember { mutableStateOf<UnitBranch?>(null) }
    var editingPriceBranch by remember { mutableStateOf<UnitBranch?>(null) }
    var addingUnitProduct by remember { mutableStateOf<Product?>(null) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var hidingProduct by remember { mutableStateOf<Product?>(null) }
    var deletingProduct by remember { mutableStateOf<Product?>(null) }
    var zoomProduct by remember { mutableStateOf<Product?>(null) }
    var editingUnitLabel by remember { mutableStateOf<Product?>(null) }
    var partiallyHidingProduct by remember { mutableStateOf<Product?>(null) }
    var expandedProductIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val lowStockCount = products.sumOf { product -> product.branches.count(::isLowStock) }
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 6.dp)) {
        val source = when (productFilter) {
            ProductFilter.ARCHIVED -> archivedProducts
            ProductFilter.LOW_STOCK -> products
            ProductFilter.ALL -> when {
                recentOnly -> products.sortedByDescending { it.createdAt }.take(20)
                else -> products
            }
        }
        val filtered = source
            .filter { it.matchesUnitFilter(if (recentOnly) StockUnitFilter.ALL else unitFilter) }
            .filter {
                productFilter != ProductFilter.LOW_STOCK ||
                    it.branches.filterFor(unitFilter).any(::isLowStock)
            }
            .let { values ->
                if (recentOnly) values.sortedByDescending { it.createdAt }
                else values.sortedWith(productComparator(sortField, sortAscending, unitFilter))
            }
        LaunchedEffect(scrollToProductId, filtered) {
            val targetId = scrollToProductId ?: return@LaunchedEffect
            val targetIndex = filtered.indexOfFirst { it.id == targetId }
            if (targetIndex >= 0) {
                expandedProductIds = expandedProductIds + targetId
                gridState.animateScrollToItem(targetIndex)
                onProductScrolled()
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (recentOnly) "Sản phẩm gần đây" else "Sản phẩm",
                style = MaterialTheme.typography.titleLarge
            )
            Text("${filtered.size} mặt hàng")
        }
        if (filtered.isEmpty()) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                EmptyState(
                    when {
                        source.isEmpty() && productFilter == ProductFilter.ARCHIVED ->
                            "Chưa có sản phẩm nào được ẩn."
                        products.isEmpty() -> "Chưa có sản phẩm. Nhấn + để thêm."
                        productFilter == ProductFilter.LOW_STOCK && lowStockCount == 0 ->
                            "Không có sản phẩm sắp hết."
                        else -> "Không tìm thấy sản phẩm."
                    }
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                state = gridState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { product ->
                    ProductGridCard(
                        product = product,
                        expanded = product.id in expandedProductIds,
                        archived = productFilter == ProductFilter.ARCHIVED,
                        onExpand = {
                            expandedProductIds = if (product.id in expandedProductIds) {
                                expandedProductIds - product.id
                            } else {
                                expandedProductIds + product.id
                            }
                        },
                        onImageClick = { zoomProduct = product },
                        onEdit = { editingProduct = product },
                        onHide = { hidingProduct = product },
                        onRestore = { vm.reactivateProduct(product.id) },
                        onDelete = { deletingProduct = product },
                        onReceive = { receivingBranch = it },
                        onEditPrice = { editingPriceBranch = it },
                        onAddUnit = { addingUnitProduct = product },
                        onEditUnitLabel = { editingUnitLabel = product }
                    )
                }
            }
        }
    }
    }

    receivingBranch?.let { branch ->
        StockInDialog(
            branch = branch,
            onDismiss = { receivingBranch = null },
            onSave = { count, cost ->
                vm.receiveStock(branch.id, count, cost)
                receivingBranch = null
            }
        )
    }
    editingPriceBranch?.let { branch ->
        PriceDialog(
            branch = branch,
            onDismiss = { editingPriceBranch = null },
            onSave = { price ->
                vm.updateSellPrice(branch.id, price)
                editingPriceBranch = null
            }
        )
    }
    addingUnitProduct?.let { product ->
        AddUnitDialog(
            product = product,
            onDismiss = { addingUnitProduct = null },
            onSave = { unitType, customName ->
                vm.addUnitBranch(product.id, unitType, customName)
                addingUnitProduct = null
            }
        )
    }
    editingProduct?.let { product ->
        ProductDialog(
            vm = vm,
            product = product,
            onDismiss = { editingProduct = null }
        )
    }
    hidingProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { hidingProduct = null },
            title = { DialogTitleWithClose("Ẩn sản phẩm?") { hidingProduct = null } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "“${product.name}” sẽ không còn trong danh sách bán hàng. " +
                            "Lịch sử đơn hàng và tồn kho được giữ lại."
                    )
                    OutlinedButton(
                        onClick = {
                            hidingProduct = null
                            partiallyHidingProduct = product
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ẩn một phần tồn kho")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.deactivateProduct(product.id)
                    hidingProduct = null
                }) { Text("Ẩn sản phẩm") }
            }
        )
    }
    partiallyHidingProduct?.let { product ->
        PartialHideDialog(
            product = product,
            onDismiss = { partiallyHidingProduct = null },
            onSave = { branchId, quantity ->
                vm.hidePartialStock(product.id, branchId, quantity)
                partiallyHidingProduct = null
            }
        )
    }
    deletingProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { deletingProduct = null },
            title = { DialogTitleWithClose("Xóa hẳn sản phẩm?") { deletingProduct = null } },
            text = {
                Text(
                    "Ảnh và sản phẩm “${product.name}” sẽ bị gỡ khỏi danh sách khôi phục. " +
                        "Đơn hàng cũ vẫn còn dòng chữ tên sản phẩm; các bản ghi kho cần thiết để giữ lịch sử sẽ được lưu."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.permanentlyDeleteProduct(product.id)
                    deletingProduct = null
                }) { Text("Xóa hẳn") }
            },
            dismissButton = {
                TextButton(onClick = { deletingProduct = null }) { Text("Hủy") }
            }
        )
    }
    zoomProduct?.let { product ->
        ProductImageViewer(
            product = product,
            onDismiss = { zoomProduct = null }
        )
    }
    editingUnitLabel?.let { product ->
        UnitSummaryLabelDialog(
            product = product,
            onDismiss = { editingUnitLabel = null },
            onSave = {
                vm.updateUnitSummaryLabel(product.id, it)
                editingUnitLabel = null
            }
        )
    }
}

@Composable
private fun UnitSummaryLabelDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var label by remember(product.id) { mutableStateOf(product.unitSummaryLabel) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleWithClose("Đổi nhãn loại", onDismiss) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Xóa hết nội dung để ẩn cả nhãn và số loại; dữ liệu các loại vẫn được giữ.")
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Nhãn hiển thị") }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(label.trim()) }) { Text("Lưu") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}

@Composable
private fun PartialHideDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (branchId: String, quantity: String) -> Unit
) {
    var selectedBranchId by remember(product.id) {
        mutableStateOf(product.branches.firstOrNull()?.id.orEmpty())
    }
    var quantityValue by remember(product.id) { mutableStateOf("") }
    val selectedBranch = product.branches.firstOrNull { it.id == selectedBranchId }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleWithClose("Ẩn một phần tồn kho") { onDismiss() } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Số lượng sẽ giảm khỏi tồn kho sẵn có; sản phẩm vẫn hiển thị trong danh sách."
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    product.branches.forEach { branch ->
                        FilterChip(
                            selected = branch.id == selectedBranchId,
                            onClick = {
                                selectedBranchId = branch.id
                                quantityValue = ""
                            },
                            label = { Text(branch.name) }
                        )
                    }
                }
                selectedBranch?.let { branch ->
                    Text("Tồn hiện có: ${quantity(branch.stock, branch.unitType)}")
                    OutlinedTextField(
                        value = quantityValue,
                        onValueChange = { quantityValue = it },
                        label = { Text("Số lượng cần ẩn (${branch.name})") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (branch.unitType == UnitType.PIECE) {
                                KeyboardType.Number
                            } else KeyboardType.Decimal
                        ),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = selectedBranch != null && quantityValue.isNotBlank(),
                onClick = {
                    selectedBranch?.let { onSave(it.id, quantityValue) }
                }
            ) { Text("Ẩn số lượng") }
        }
    )
}

@Composable
private fun ProductGridCard(
    product: Product,
    expanded: Boolean,
    archived: Boolean,
    onExpand: () -> Unit,
    onImageClick: () -> Unit,
    onEdit: () -> Unit,
    onHide: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    onReceive: (UnitBranch) -> Unit,
    onEditPrice: (UnitBranch) -> Unit,
    onAddUnit: () -> Unit,
    onEditUnitLabel: () -> Unit
) {
    val primaryBranch = product.branches.firstOrNull()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        )
    ) {
        Column(Modifier.padding(8.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable(enabled = product.imagePath != null, onClick = onImageClick)
        ) {
            ProductImage(product.imagePath, Modifier.fillMaxSize())
            primaryBranch?.let { branch ->
                Text(
                    quantity(branch.stock, branch.unitType),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            stockStatusColor(branch).takeUnless { it == Color.Unspecified }
                                ?: MaterialTheme.colorScheme.primary
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            product.name,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 34.dp)
                    .pointerInput(product.id, imageIndex) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val held = withTimeoutOrNull(2_000L) {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    if (
                                        event.changes.firstOrNull { it.id == down.id }?.pressed != true
                                    ) {
                                        return@withTimeoutOrNull
                                    }
                                }
                            }
                            if (held == null) onEditUnitLabel()
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                if (product.unitSummaryLabel.isNotBlank()) {
                    Text(
                        "${product.branches.size} ${product.unitSummaryLabel}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = onExpand, modifier = Modifier.size(34.dp)) {
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Thu gọn chi tiết" else "Xem chi tiết"
                )
            }
            if (archived) {
                IconButton(onClick = onRestore, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Restore, contentDescription = "Khôi phục")
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Xóa hẳn")
                }
            } else {
                IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa sản phẩm")
                }
                IconButton(onClick = onHide, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Ẩn sản phẩm")
                }
            }
        }
            if (expanded) {
                if (archived) {
                    val dayMillis = 24L * 60 * 60 * 1000
                    val daysRemaining = product.archivedAt?.let { archivedAt ->
                        val remaining = (
                            archivedAt + 30L * dayMillis - System.currentTimeMillis()
                            ).coerceAtLeast(0L)
                        ((remaining + dayMillis - 1) / dayMillis).toInt()
                    } ?: 30
                    Text(
                        if (daysRemaining == 0) {
                            "Sản phẩm sẽ được tự động xóa trong hôm nay."
                        } else {
                            "Còn $daysRemaining ngày sẽ tự xóa sản phẩm và ảnh."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    product.branches.forEach { branch ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(branch.name, style = MaterialTheme.typography.labelLarge)
                                Text(
                                    quantity(branch.stock, branch.unitType),
                                    color = if (isLowStock(branch)) MaterialTheme.colorScheme.error
                                    else MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Text(
                                "Nhập ${money(branch.buyPrice)} · Bán ${money(branch.sellPrice)}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                TextButton(onClick = { onReceive(branch) }) {
                                    Text("Nhập")
                                }
                                TextButton(onClick = { onEditPrice(branch) }) {
                                    Text("Sửa giá")
                                }
                            }
                        }
                    }
                    TextButton(onClick = onAddUnit, modifier = Modifier.align(Alignment.End)) {
                        Text("Thêm đơn vị")
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductImageViewer(product: Product, onDismiss: () -> Unit) {
    val imagePaths = remember(product.id, product.imagePath, product.additionalImagePaths) {
        listOfNotNull(product.imagePath) + product.additionalImagePaths
    }
    var imageIndex by remember(product.id) { mutableIntStateOf(0) }
    val currentImagePath = imagePaths.getOrNull(imageIndex)
    val bitmap = remember(currentImagePath) {
        currentImagePath?.let { BitmapFactory.decodeFile(it) }
    }
    var scale by remember(product.id, imageIndex) { mutableFloatStateOf(1f) }
    var pan by remember(product.id, imageIndex) { mutableStateOf(Offset.Zero) }
    var showDetails by remember(product.id) { mutableStateOf(false) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            Modifier.fillMaxSize().background(Color.Black)
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = product.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(product.id) {
                            detectTransformGestures { _, panChange, zoomChange, _ ->
                                scale = (scale * zoomChange).coerceIn(1f, 5f)
                                pan = if (scale == 1f) Offset.Zero else pan + panChange
                            }
                        }
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = pan.x
                            translationY = pan.y
                        },
                    contentScale = ContentScale.Fit
                )
            }
            if (imagePaths.size > 1) {
                IconButton(
                    onClick = {
                        imageIndex = (imageIndex - 1 + imagePaths.size) % imagePaths.size
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowLeft,
                        contentDescription = "Ảnh trước",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = { imageIndex = (imageIndex + 1) % imagePaths.size },
                    modifier = Modifier.align(Alignment.CenterEnd)
                        .padding(12.dp)
                        .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowRight,
                        contentDescription = "Ảnh tiếp theo",
                        tint = Color.White
                    )
                }
                Text(
                    "${imageIndex + 1} / ${imagePaths.size}",
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp)
                        .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.48f))
                    .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    product.name,
                    modifier = Modifier.weight(1f),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Đóng ảnh", tint = Color.White)
                }
            }
            product.branches.firstOrNull()?.let { branch ->
                Text(
                    quantity(branch.stock, branch.unitType),
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 76.dp, end = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
            ) {
                TextButton(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier.align(Alignment.Start)
                ) {
                    Icon(
                        if (showDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Text(if (showDetails) "Ẩn chi tiết" else "Chi tiết", color = Color.White)
                }
                if (showDetails) {
                    product.branches.forEach { branch ->
                        Text(
                            "${branch.name} · Tồn ${quantity(branch.stock, branch.unitType)} · " +
                                "Nhập ${money(branch.buyPrice)} · Bán ${money(branch.sellPrice)}",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StockInDialog(
    branch: UnitBranch,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var count by remember(branch.id) { mutableStateOf("") }
    var cost by remember(branch.id) { mutableStateOf(if (branch.buyPrice > 0) branch.buyPrice.toString() else "") }
    var quantityUnit by remember(branch.id) { mutableStateOf("kg") }
    var error by remember(branch.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleWithClose("Nhập ${branch.name}", onDismiss) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    count,
                    { count = it },
                    label = {
                        Text(if (branch.unitType == UnitType.WEIGHT) "Số lượng (${quantityUnit})" else "Số lượng")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (branch.unitType == UnitType.WEIGHT) {
                            KeyboardType.Decimal
                        } else {
                            KeyboardType.Number
                        }
                    )
                )
                if (branch.unitType == UnitType.WEIGHT) {
                    TextButton(onClick = { quantityUnit = if (quantityUnit == "kg") "g" else "kg" }) {
                        Text("Đổi đơn vị nhập: $quantityUnit")
                    }
                }
                CurrencyTextField(
                    value = cost,
                    onValueChange = { cost = it },
                    label = if (branch.unitType == UnitType.WEIGHT) "Giá nhập / kg" else "Giá nhập mỗi đơn vị"
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedCount = count.toDoubleOrNull()
                val parsedCost = parseCurrencyInput(cost)
                when {
                    parsedCount == null || !parsedCount.isFinite() || parsedCount <= 0.0 -> {
                        error = "Số lượng phải lớn hơn 0."
                    }
                    parsedCost == null || parsedCost < 0L -> {
                        error = "Giá nhập không hợp lệ."
                    }
                    else -> {
                        error = null
                        val normalizedCount = if (branch.unitType == UnitType.WEIGHT && quantityUnit == "g") {
                            (parsedCount / 1000.0).toString()
                        } else {
                            count
                        }
                        onSave(normalizedCount, cost)
                    }
                }
            }) { Text("Lưu nhập") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun PriceDialog(
    branch: UnitBranch,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var price by remember(branch.id) { mutableStateOf(branch.sellPrice.toString()) }
    var error by remember(branch.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleWithClose("Giá bán · ${branch.name}", onDismiss) },
        text = {
            CurrencyTextField(
                value = price,
                onValueChange = { price = it },
                label = "Giá bán mỗi đơn vị"
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        },
        confirmButton = {
            TextButton(onClick = {
                val parsedPrice = parseCurrencyInput(price)
                error = if (parsedPrice == null || parsedPrice < 0L) {
                    "Giá bán không hợp lệ."
                } else {
                    null
                }
                if (error == null) onSave(price)
            }) { Text("Lưu giá") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun AddUnitDialog(
    product: Product,
    onDismiss: () -> Unit,
    onSave: (UnitType, String) -> Unit
) {
    var unitType by remember(product.id) { mutableStateOf(UnitType.WEIGHT) }
    var customName by remember(product.id) { mutableStateOf("") }
    var error by remember(product.id) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleWithClose("Thêm đơn vị · ${product.name}", onDismiss) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row {
                    RadioButton(unitType == UnitType.PIECE, { unitType = UnitType.PIECE })
                    Text("Cái", Modifier.padding(top = 12.dp))
                }
                Row {
                    RadioButton(unitType == UnitType.WEIGHT, { unitType = UnitType.WEIGHT })
                    Text("Kg/gam", Modifier.padding(top = 12.dp))
                }
                Row {
                    RadioButton(unitType == UnitType.OTHER, { unitType = UnitType.OTHER })
                    Text("Đơn vị khác", Modifier.padding(top = 12.dp))
                }
                if (unitType == UnitType.OTHER) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Tên đơn vị") },
                        singleLine = true
                    )
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = if (unitType == UnitType.OTHER && customName.isBlank()) {
                    "Nhập tên đơn vị."
                } else {
                    null
                }
                if (error == null) onSave(unitType, customName)
            }) { Text("Thêm đơn vị") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SalesScreen(products: List<Product>, orders: List<SalesOrder>, vm: KhoViewModel) {
    var menuExpanded by remember { mutableStateOf(false) }
    var showOrders by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (showOrders) "Đơn hàng" else "Tạo đơn bán",
                style = MaterialTheme.typography.headlineSmall
            )
            Box {
                TextButton(onClick = { menuExpanded = true }) {
                    Text(if (showOrders) "Đơn hàng" else "Tạo đơn")
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Chuyển màn hình")
                }
                DropdownMenu(menuExpanded, { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Tạo đơn bán") },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        onClick = {
                            showOrders = false
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Lịch sử đơn hàng") },
                        leadingIcon = { Icon(Icons.Default.ReceiptLong, null) },
                        onClick = {
                            showOrders = true
                            menuExpanded = false
                        }
                    )
                }
            }
        }
        if (showOrders) {
            OrdersScreen(orders, vm)
        } else {
            SaleForm(products, vm, Modifier.weight(1f))
        }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SaleForm(products: List<Product>, vm: KhoViewModel, modifier: Modifier = Modifier) {
    val branches = products.flatMap { product ->
        product.branches.map { branch -> product to branch }
    }
    var productSearch by remember { mutableStateOf("") }
    val selected = branches.firstOrNull { it.second.id == vm.selectedBranchId }

    LaunchedEffect(branches) {
        if (vm.selectedBranchId.isBlank() || branches.none { it.second.id == vm.selectedBranchId }) {
            vm.selectedBranchId = branches.firstOrNull()?.second?.id.orEmpty()
        }
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (branches.isEmpty()) {
            item { EmptyState("Hãy thêm sản phẩm trước khi tạo đơn.") }
        } else {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(
                        value = productSearch,
                        onValueChange = {
                            productSearch = it
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Tìm sản phẩm") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (productSearch.isNotEmpty()) {
                                IconButton(onClick = { productSearch = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Xóa tìm kiếm")
                                }
                            } else {
                                Icon(Icons.Default.Warehouse, contentDescription = null)
                            }
                        },
                        singleLine = true
                    )
                    selected?.let { (product, branch) ->
                        Text(
                            "Đang chọn: ${product.name} · ${branch.name} · " +
                                "Tồn ${quantity(branch.stock, branch.unitType)}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    val matchingBranches = branches.filter { (product, branch) ->
                        productSearch.isNotBlank() &&
                            (product.name.contains(productSearch, ignoreCase = true) ||
                                branch.name.contains(productSearch, ignoreCase = true))
                    }.take(10)
                    if (productSearch.isNotBlank()) {
                        if (matchingBranches.isEmpty()) {
                            Text("Không tìm thấy sản phẩm.")
                        } else {
                            Column(
                                modifier = Modifier.heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                matchingBranches.forEach { (product, branch) ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            vm.selectedBranchId = branch.id
                                            vm.saleQuantityUnit = "kg"
                                            vm.saleUnitPrice = formatCurrencyInput(branch.sellPrice.toString())
                                            productSearch = ""
                                        }
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth().padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ProductImage(product.imagePath, Modifier.size(44.dp))
                                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                                Text("${product.name} · ${branch.name}")
                                                Text(
                                                    "Tồn ${quantity(branch.stock, branch.unitType)} · ${money(branch.sellPrice)}",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            Icon(Icons.Default.Add, contentDescription = "Chọn sản phẩm")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = vm.saleQuantity,
                            onValueChange = { vm.saleQuantity = it },
                            modifier = Modifier.weight(1f),
                            label = {
                                Text(
                                    if (selected?.second?.unitType == UnitType.WEIGHT) {
                                        "Số lượng (${vm.saleQuantityUnit})"
                                    } else {
                                        "Số lượng"
                                    }
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = if (selected?.second?.unitType == UnitType.WEIGHT) {
                                    KeyboardType.Decimal
                                } else {
                                    KeyboardType.Number
                                }
                            )
                        )
                        if (selected?.second?.unitType == UnitType.WEIGHT) {
                            TextButton(
                                onClick = {
                                    vm.saleQuantityUnit = if (vm.saleQuantityUnit == "kg") "g" else "kg"
                                }
                            ) {
                                Text(vm.saleQuantityUnit)
                            }
                        }
                    }
                    CurrencyTextField(
                        value = vm.saleUnitPrice.ifBlank {
                            selected?.second?.sellPrice?.let { formatCurrencyInput(it.toString()) }.orEmpty()
                        },
                        onValueChange = { vm.saleUnitPrice = it },
                        label = if (selected?.second?.unitType == UnitType.WEIGHT) "Giá / kg" else "Giá bán"
                    )
                }
            }
            item {
                Button(
                    onClick = {
                        if (vm.saleUnitPrice.isBlank()) {
                            vm.saleUnitPrice = selected?.second?.sellPrice?.toString().orEmpty()
                        }
                        vm.addSaleLine()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("Thêm dòng hàng")
                }
            }
            if (vm.saleLines.isNotEmpty()) {
                item { Text("Sản phẩm trong đơn", style = MaterialTheme.typography.titleMedium) }
                items(vm.saleLines.size) { index ->
                    val line = vm.saleLines[index]
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text("${line.productName} · ${line.branchName}")
                                Text(
                                    "${quantity(line.quantity, line.unitType)} × ${money(line.unitPrice)} = ${money(line.total)}"
                                )
                            }
                            TextButton(onClick = { vm.removeSaleLine(index) }) { Text("Bỏ") }
                        }
                    }
                }
                item {
                    val total = vm.saleLines.sumOf { it.total }
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tổng đơn: ${money(total)}", style = MaterialTheme.typography.titleLarge)
                        OutlinedTextField(
                            value = vm.saleCustomer,
                            onValueChange = { vm.saleCustomer = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Khách hàng (để trống = Khách lẻ)") },
                            singleLine = true
                        )
                        CurrencyTextField(
                            value = vm.salePayment,
                            onValueChange = { vm.salePayment = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = "Đã thu (để trống = chưa thu)"
                        )
                        Button(onClick = vm::createSale, modifier = Modifier.fillMaxWidth()) {
                            Text("Lưu đơn và xuất kho")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrdersScreen(orders: List<SalesOrder>, vm: KhoViewModel) {
    var paymentOrder by remember { mutableStateOf<SalesOrder?>(null) }
    var cancelOrder by remember { mutableStateOf<SalesOrder?>(null) }
    var paymentAmount by remember { mutableStateOf("") }
    var paymentError by remember { mutableStateOf<String?>(null) }
    if (orders.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            EmptyState("Chưa có đơn hàng.")
        }
    } else {
        LazyColumn(
            Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(orders, key = { it.id }) { order ->
                OrderCard(
                    order = order,
                    vm = vm,
                    onPayment = {
                        paymentOrder = order
                        paymentAmount = formatCurrencyInput(
                            (order.totalAmount - order.paidAmount).coerceAtLeast(0L).toString()
                        )
                        paymentError = null
                    },
                    onCancel = { cancelOrder = order },
                    onRefund = { vm.confirmRefund(order.id) }
                )
            }
        }
    }
    paymentOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { paymentOrder = null },
            title = { DialogTitleWithClose("Ghi nhận thanh toán") { paymentOrder = null } },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Còn nợ ${money((order.totalAmount - order.paidAmount).coerceAtLeast(0L))}")
                    CurrencyTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = "Số tiền nhận"
                    )
                    paymentError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val amount = parseCurrencyInput(paymentAmount)
                    val remaining = order.totalAmount - order.paidAmount
                    paymentError = if (amount == null || amount <= 0L || amount > remaining) {
                        "Nhập số tiền lớn hơn 0 và không vượt số còn nợ."
                    } else {
                        null
                    }
                    if (paymentError == null) {
                        vm.receivePayment(order.id, paymentAmount)
                        paymentOrder = null
                    }
                }) { Text("Lưu thanh toán") }
            },
            dismissButton = { TextButton(onClick = { paymentOrder = null }) { Text("Hủy") } }
        )
    }
    cancelOrder?.let { order ->
        AlertDialog(
            onDismissRequest = { cancelOrder = null },
            title = { DialogTitleWithClose("Hủy đơn hàng?") { cancelOrder = null } },
            text = {
                Text(
                    "Tồn kho sẽ được hoàn lại ngay. Khoản đã thu ${money(order.paidAmount)} " +
                        "sẽ chờ xác nhận hoàn tiền riêng."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.cancelOrder(order.id)
                    cancelOrder = null
                }) { Text("Hủy đơn") }
            },
            dismissButton = { TextButton(onClick = { cancelOrder = null }) { Text("Quay lại") } }
        )
    }
}

@Composable
private fun OrderCard(
    order: SalesOrder,
    vm: KhoViewModel,
    onPayment: () -> Unit,
    onCancel: () -> Unit,
    onRefund: () -> Unit
) {
    val lines by remember(order.id) {
        vm.observeOrderLines(order.id)
    }.collectAsState(initial = emptyList())
    GlassCard(Modifier.fillMaxWidth()) {
        Text(order.customerName, style = MaterialTheme.typography.titleMedium)
        Text("${dateTime(order.createdAt)} · ${orderStatus(order.status)}")
        lines.forEach { line ->
            Text(
                "${line.productName} (${line.branchName}) × ${quantity(line.quantity)}" +
                    if (line.productDeleted) " · đã xóa" else ""
            )
        }
        Text("Tổng: ${money(order.totalAmount)}")
        if (order.status != "CANCELLED") {
            Text("Đã thu: ${money(order.paidAmount)} · Còn nợ: ${money((order.totalAmount - order.paidAmount).coerceAtLeast(0L))}")
        }
        if (order.status == "CANCELLED" && order.refundPending > 0L) {
            Text("Chờ xác nhận hoàn ${money(order.refundPending)}")
            TextButton(onClick = onRefund) { Text("Xác nhận đã hoàn tiền") }
        } else if (order.status != "CANCELLED") {
            if (order.paidAmount < order.totalAmount) {
                TextButton(onClick = onPayment) { Text("Thu tiền") }
            }
            TextButton(onClick = onCancel) { Text("Hủy đơn") }
        }
    }
}

@Composable
private fun Reports(
    products: List<Product>,
    orders: List<SalesOrder>,
    unreadLowStockCount: Int,
    onMarkLowStockRead: () -> Unit
) {
    var summaryExpanded by remember { mutableStateOf(true) }
    val activeOrders = orders.filter { it.status != "CANCELLED" }
    val totalReceived = activeOrders.sumOf { it.paidAmount }
    val receivable = activeOrders.sumOf { (it.totalAmount - it.paidAmount).coerceAtLeast(0L) }
    val stockValue = products.sumOf { product ->
        product.branches.sumOf { calculateAmount(it.stock, it.buyPrice) }
    }
    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { Text("Thông báo & báo cáo", style = MaterialTheme.typography.headlineMedium) }
        item {
            val lowStock = products.flatMap { product ->
                product.branches.filter(::isLowStock).map { branch -> product.name to branch }
            }
            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Cần chú ý",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (unreadLowStockCount > 0) {
                        IconButton(
                            onClick = onMarkLowStockRead,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Đánh dấu cảnh báo đã đọc"
                            )
                        }
                    }
                }
                if (lowStock.isEmpty()) {
                    Text("Không có mặt hàng nào sắp hết.")
                } else {
                    lowStock.forEach { (name, branch) ->
                        Text(
                            "$name · ${branch.name}: ${quantity(branch.stock, branch.unitType)}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Tổng hợp",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = { summaryExpanded = !summaryExpanded }) {
                    Icon(
                        if (summaryExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (summaryExpanded) "Thu gọn tổng hợp" else "Mở tổng hợp"
                    )
                }
            }
        }
        if (summaryExpanded) {
            item { Metric("Số đơn", orders.size.toString()) }
            item { Metric("Đã thu", money(totalReceived)) }
            item { Metric("Công nợ", money(receivable)) }
            item { Metric("Giá trị tồn theo giá nhập gần nhất", money(stockValue)) }
        }
    }
}

@Composable
private fun UserAvatar(photo: String?, modifier: Modifier = Modifier) {
    val model = remember(photo) {
        when {
            photo.isNullOrBlank() -> null
            photo.startsWith("http://") || photo.startsWith("https://") ||
                photo.startsWith("content://") || photo.startsWith("file://") ->
                Uri.parse(photo)
            else -> File(photo)
        }
    }
    Box(
        modifier = modifier.clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (model == null) {
            Icon(
                Icons.Default.Person,
                contentDescription = "Ảnh tài khoản",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.fillMaxSize().padding(8.dp)
            )
        } else {
            AsyncImage(
                model = model,
                contentDescription = "Ảnh tài khoản",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun AccountChooserDialog(
    displayName: String?,
    email: String?,
    photo: String?,
    localOnly: Boolean,
    canSignIn: Boolean,
    onDismiss: () -> Unit,
    onAddAccount: () -> Unit,
    onEnableCodeSync: (() -> Unit)?,
    onSignOut: (() -> Unit)?
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Tài khoản và đồng bộ",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng tài khoản")
                    }
                }
                if (!localOnly) {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        UserAvatar(photo, Modifier.size(56.dp))
                        Column {
                            Text(displayName ?: "Tài khoản thiết bị", style = MaterialTheme.typography.titleMedium)
                            email?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                } else {
                    Text("Bạn đang dùng kho ngoại tuyến trên thiết bị này.")
                    Text(
                        "Tài khoản đồng bộ gắn với thiết bị này. Không xóa dữ liệu ứng dụng trước khi có cách khôi phục tài khoản.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    onEnableCodeSync?.let { enableSync ->
                        TextButton(
                            onClick = enableSync,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Bật đồng bộ qua mã (không cần Google)")
                        }
                    } ?: Text("Đồng bộ qua Internet cần cấu hình Firebase.")
                }
                if (canSignIn) {
                    TextButton(
                        onClick = onAddAccount,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.size(10.dp))
                        Text(if (localOnly) "Đăng nhập Google" else "Thêm hoặc đổi tài khoản")
                    }
                } else if (localOnly) {
                    Text("Đăng nhập Google sẽ được hoàn thiện sau.")
                }
                if (!localOnly && onSignOut != null) {
                    HorizontalDivider()
                    TextButton(onClick = { onSignOut() }, modifier = Modifier.fillMaxWidth()) {
                        Text("Đăng xuất")
                    }
                }
            }
        }
    }
}

@Composable
private fun RenameWarehouseDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(currentName) { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleWithClose("Đổi tên kho", onDismiss) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tên kho") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onSave(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun InventoryImportDialog(
    imported: List<InventoryBackupProduct>,
    existingProducts: List<Product>,
    onDismiss: () -> Unit,
    onConfirm: (List<InventoryImportDecision>) -> Unit
) {
    val existingNames = existingProducts.map { normalizeProductName(it.name).lowercase(Locale.ROOT) }
    var decisions by remember(imported, existingProducts) {
        mutableStateOf(
            imported.map { product ->
                val duplicate = normalizeProductName(product.name).lowercase(Locale.ROOT) in existingNames
                InventoryImportDecision(
                    product = product,
                    action = if (duplicate) DuplicateImportAction.SKIP else DuplicateImportAction.CREATE_NEW,
                    newName = if (duplicate) suggestedProductName(product.name, existingNames) else product.name
                )
            }
        )
    }
    val newNames = decisions
        .filter { it.action == DuplicateImportAction.CREATE_NEW }
        .mapNotNull { it.newName?.let(::normalizeProductName) }
    val hasNameConflict = decisions.any {
        it.action == DuplicateImportAction.CREATE_NEW && it.newName.isNullOrBlank()
    } || newNames.map { it.lowercase(Locale.ROOT) }.distinct().size != newNames.size ||
        newNames.any { it.lowercase(Locale.ROOT) in existingNames }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleWithClose("Kiểm tra sản phẩm nhập", onDismiss) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Chọn cách xử lý cho từng sản phẩm trùng tên.")
                decisions.forEachIndexed { index, decision ->
                    val duplicate = normalizeProductName(decision.product.name)
                        .lowercase(Locale.ROOT) in existingNames
                    GlassCard(Modifier.fillMaxWidth()) {
                        Text(decision.product.name, style = MaterialTheme.typography.titleSmall)
                        if (duplicate) {
                            listOf(
                                DuplicateImportAction.SKIP to "Bỏ qua",
                                DuplicateImportAction.KEEP_CURRENT_PRICES to "Cập nhật tồn, giữ giá hiện tại",
                                DuplicateImportAction.CREATE_NEW to "Tạo sản phẩm mới"
                            ).forEach { (action, label) ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = decision.action == action,
                                        onClick = {
                                            val updated = decisions.toMutableList()
                                            updated[index] = decision.copy(
                                                action = action,
                                                newName = if (action == DuplicateImportAction.CREATE_NEW) {
                                                    suggestedProductName(decision.product.name, existingNames)
                                                } else {
                                                    decision.newName
                                                }
                                            )
                                            decisions = updated
                                        }
                                    )
                                    Text(label)
                                }
                            }
                        } else {
                            Text("Sẽ thêm sản phẩm mới.")
                        }
                        if (decision.action == DuplicateImportAction.CREATE_NEW) {
                            OutlinedTextField(
                                value = decision.newName.orEmpty(),
                                onValueChange = { value ->
                                    val updated = decisions.toMutableList()
                                    updated[index] = decision.copy(newName = value)
                                    decisions = updated
                                },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Tên sản phẩm mới") },
                                singleLine = true
                            )
                        }
                    }
                }
                if (hasNameConflict) {
                    Text(
                        "Tên mới phải khác sản phẩm đang có và không được trùng nhau.",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(decisions) },
                enabled = !hasNameConflict && decisions.any { it.action != DuplicateImportAction.SKIP }
            ) { Text("Nhập dữ liệu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

private fun suggestedProductName(name: String, existingNames: List<String>): String {
    var version = 1
    var suggestion: String
    do {
        suggestion = "$name v$version"
        version++
    } while (normalizeProductName(suggestion).lowercase(Locale.ROOT) in existingNames)
    return suggestion
}

@Composable
private fun ColorSettingButton(
    title: String,
    color: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Box(
            Modifier.size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
        )
        Text(title, modifier = Modifier.weight(1f).padding(start = 10.dp), maxLines = 1)
        Icon(Icons.Default.Edit, contentDescription = "Chỉnh $title")
    }
}

@Composable
private fun ColorPickerDialog(
    initialHex: String,
    title: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit
) {
    val initialColor = colorFromHex(initialHex) ?: Color(0xFF0284C7)
    val hsv = remember(initialHex) {
        FloatArray(3).also {
            android.graphics.Color.colorToHSV(initialColor.toArgb(), it)
        }
    }
    var hue by remember(initialHex) { mutableFloatStateOf(hsv[0]) }
    var saturation by remember(initialHex) { mutableFloatStateOf(hsv[1]) }
    var brightness by remember(initialHex) { mutableFloatStateOf(hsv[2]) }
    var alpha by remember(initialHex) { mutableFloatStateOf(initialColor.alpha) }
    val selectedColor = Color(
        android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, brightness))
    ).copy(alpha = alpha)

    fun updateHue(position: Float, width: Float) {
        hue = (position / width * 360f).coerceIn(0f, 360f)
    }

    fun updateTone(position: Offset, width: Float, height: Float) {
        saturation = (position.x / width).coerceIn(0f, 1f)
        brightness = (1f - position.y / height).coerceIn(0f, 1f)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { DialogTitleWithClose(title, onDismiss) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Canvas(
                    Modifier.fillMaxWidth().height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .pointerInput(hue) {
                            detectTapGestures { updateTone(it, size.width.toFloat(), size.height.toFloat()) }
                        }
                        .pointerInput(hue) {
                            detectDragGestures(
                                onDragStart = {
                                    updateTone(it, size.width.toFloat(), size.height.toFloat())
                                }
                            ) { change, _ ->
                                updateTone(change.position, size.width.toFloat(), size.height.toFloat())
                                change.consume()
                            }
                        }
                ) {
                    val hueColor = Color(
                        android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f))
                    )
                    drawRect(color = hueColor)
                    drawRect(brush = Brush.horizontalGradient(listOf(Color.White, Color.Transparent)))
                    drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                }
                Canvas(
                    Modifier.fillMaxWidth().height(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .pointerInput(Unit) {
                            detectTapGestures { updateHue(it.x, size.width.toFloat()) }
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { updateHue(it.x, size.width.toFloat()) }
                            ) { change, _ ->
                                updateHue(change.position.x, size.width.toFloat())
                                change.consume()
                            }
                        }
                ) {
                    val colors = (0..6).map { step ->
                        Color(android.graphics.Color.HSVToColor(floatArrayOf(step * 60f, 1f, 1f)))
                    }
                    drawRect(brush = Brush.horizontalGradient(colors))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Độ đục ${(alpha * 100).toInt()}%", modifier = Modifier.weight(1f))
                    Box(
                        Modifier.size(28.dp).clip(RoundedCornerShape(6.dp))
                            .background(selectedColor)
                    )
                }
                Slider(value = alpha, onValueChange = { alpha = it })
                OutlinedTextField(
                    value = String.format(Locale.ROOT, "#%08X", selectedColor.toArgb()),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Mã màu ARGB") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onApply(String.format(Locale.ROOT, "#%08X", selectedColor.toArgb()))
                }
            ) { Text("Dùng màu này") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } }
    )
}

@Composable
private fun SettingsScreen(
    appName: String,
    canCreateShareCode: Boolean,
    shareCodeEnabled: Boolean,
    shareCode: String,
    savedShareCode: String?,
    onShareCodeInputChange: (String) -> Unit,
    onShareCodeEnabledChange: (Boolean) -> Unit,
    onCreateShareCode: (suspend (String?) -> String)?,
    onShareCodeCreated: (String) -> Unit,
    onLoadShareRequests: (suspend () -> List<String>)?,
    onDecideShareRequest: (suspend (String, Boolean) -> Unit)?,
    onReceiveNearbyShareCode: () -> Unit,
    accountDisplayName: String?,
    accountEmail: String?,
    accountPhoto: String?,
    localOnly: Boolean,
    syncError: String?,
    onRetrySync: () -> Unit,
    canChangeAccount: Boolean,
    onRequestGoogleSignIn: (() -> Unit)?,
    onEnableCodeSync: (() -> Unit)?,
    onSignOut: (() -> Unit)?,
    onPhotoChange: (String?) -> Unit,
    appearance: AppAppearance,
    products: List<Product>,
    onAppNameChange: (String) -> Unit,
    onAppearanceChange: (AppAppearance) -> Unit,
    onBackupMessage: (String) -> Unit,
    onImport: (List<InventoryImportDecision>) -> Unit,
    onDeleteAllProducts: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var backgroundHex by remember(appearance.backgroundHex) {
        mutableStateOf(appearance.backgroundHex)
    }
    var buttonHex by remember(appearance.buttonHex) {
        mutableStateOf(appearance.buttonHex)
    }
    var secondaryHex by remember(appearance.secondaryHex) {
        mutableStateOf(appearance.secondaryHex)
    }
    var textHex by remember(appearance.textHex) {
        mutableStateOf(appearance.textHex)
    }
    var appearanceExpanded by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var creatingShareCode by remember { mutableStateOf(false) }
    var showNearbyShareDialog by remember { mutableStateOf(false) }
    var shareRequests by remember { mutableStateOf<List<String>>(emptyList()) }
    var loadingShareRequests by remember { mutableStateOf(false) }
    var decidingShareRequest by remember { mutableStateOf<String?>(null) }
    var pendingImport by remember { mutableStateOf<List<InventoryBackupProduct>?>(null) }
    var pickerTarget by remember { mutableStateOf<String?>(null) }
    var confirmDeleteAll by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                working = true
                try {
                    withContext(Dispatchers.IO) {
                        InventoryBackup.write(context, uri, products)
                    }
                    onBackupMessage("Đã xuất ZIP gồm sản phẩm, số lượng, giá và ảnh trong kho.")
                } catch (failure: IOException) {
                    onBackupMessage("Không xuất được ZIP: ${failure.localizedMessage ?: "lỗi tệp"}")
                } finally {
                    working = false
                }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                working = true
                try {
                    val imported = withContext(Dispatchers.IO) {
                        InventoryBackup.read(context, uri)
                    }
                    pendingImport = imported
                } catch (failure: IOException) {
                    onBackupMessage("Không nhập được ZIP: ${failure.localizedMessage ?: "lỗi tệp"}")
                } catch (failure: IllegalArgumentException) {
                    onBackupMessage("Tệp sao lưu không hợp lệ: ${failure.localizedMessage ?: "dữ liệu sai"}")
                } catch (failure: JSONException) {
                    onBackupMessage("Tệp sao lưu thiếu dữ liệu hợp lệ.")
                } finally {
                    working = false
                }
            }
        }
    }
    val accountPhotoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val savedPath = withContext(Dispatchers.IO) {
                        val directory = File(context.filesDir, "account-photos")
                        if (!directory.exists() && !directory.mkdirs()) {
                            throw IOException("Không tạo được thư mục ảnh tài khoản.")
                        }
                        val destination = File(directory, "${UUID.randomUUID()}.jpg")
                        try {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(destination).use { output -> input.copyTo(output) }
                            } ?: throw IOException("Không đọc được ảnh đã chọn.")
                            destination.absolutePath
                        } catch (failure: IOException) {
                            destination.delete()
                            throw failure
                        } catch (failure: SecurityException) {
                            destination.delete()
                            throw failure
                        }
                    }
                    onPhotoChange(savedPath)
                } catch (failure: IOException) {
                    onBackupMessage(
                        "Không lưu được ảnh tài khoản: ${failure.localizedMessage ?: "lỗi tệp"}"
                    )
                } catch (failure: SecurityException) {
                    onBackupMessage("Không có quyền đọc ảnh tài khoản đã chọn.")
                }
            }
        }
    }

    LaunchedEffect(shareCodeEnabled, canCreateShareCode, appName) {
        val loadRequests = onLoadShareRequests ?: return@LaunchedEffect
        if (!shareCodeEnabled || !canCreateShareCode) return@LaunchedEffect
        loadingShareRequests = true
        try {
            shareRequests = loadRequests()
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (error: com.google.firebase.FirebaseException) {
            onBackupMessage(
                "Không tải được yêu cầu tham gia: ${error.localizedMessage ?: "lỗi Firebase"}"
            )
        } catch (error: IllegalStateException) {
            onBackupMessage(error.message ?: "Không tải được yêu cầu tham gia.")
        } finally {
            loadingShareRequests = false
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wideLayout = maxWidth >= 700.dp
        LazyColumn(
            Modifier.align(Alignment.TopCenter).widthIn(max = 1120.dp).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
            GlassCard(Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    UserAvatar(accountPhoto, Modifier.size(56.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Tài khoản", style = MaterialTheme.typography.titleMedium)
                        Text(accountDisplayName ?: if (localOnly) "Ngoại tuyến" else "Google")
                        accountEmail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                    }
                }
                TextButton(onClick = { accountPhotoPicker.launch(arrayOf("image/*")) }) {
                    Text(if (accountPhoto == null) "Thêm ảnh cá nhân" else "Đổi ảnh cá nhân")
                }
                if (localOnly) {
                    Text(
                        "Kho vẫn dùng được ngoại tuyến. Bật đồng bộ qua mã để kết nối thiết bị bằng Internet; không cần Google.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    onEnableCodeSync?.let { enableSync ->
                        TextButton(onClick = enableSync) {
                            Text("Bật đồng bộ qua mã")
                        }
                    }
                    onRequestGoogleSignIn?.let { signIn ->
                        TextButton(onClick = signIn) {
                            Text("Đăng nhập Google")
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (canChangeAccount) {
                            TextButton(onClick = { onRequestGoogleSignIn?.invoke() }) {
                                Text("Đổi tài khoản")
                            }
                        }
                        onSignOut?.let { signOut ->
                            TextButton(onClick = signOut) { Text("Đăng xuất") }
                        }
                    }
                }
            }
            if (wideLayout && !localOnly) {
                GlassCard(Modifier.weight(1f)) {
                    Text("Đồng bộ kho", style = MaterialTheme.typography.titleMedium)
                    if (syncError == null) {
                        Text("Đang chờ hoặc đã đồng bộ khi có mạng.")
                    } else {
                        Text(
                            "Chưa đồng bộ được: $syncError",
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = onRetrySync) { Text("Thử lại") }
                    }
                }
            }
            }
        }
        if (!localOnly && !wideLayout) {
            item {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text("Đồng bộ kho", style = MaterialTheme.typography.titleMedium)
                    if (syncError == null) {
                        Text("Đang chờ hoặc đã đồng bộ khi có mạng.")
                    } else {
                        Text(
                            "Chưa đồng bộ được: $syncError",
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(onClick = onRetrySync) { Text("Thử lại") }
                    }
                }
            }
        }
        item { Text("Cài đặt ứng dụng", style = MaterialTheme.typography.headlineSmall) }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warehouse, contentDescription = null)
                    Text(
                        "  Tên kho",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                OutlinedTextField(
                    value = appName,
                    onValueChange = onAppNameChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Tên hiển thị") },
                    singleLine = true
                )
                if (localOnly || canCreateShareCode) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Chia sẻ kho bằng mã", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Bật để tạo mã và cho phép thiết bị khác yêu cầu tham gia.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = shareCodeEnabled,
                            onCheckedChange = onShareCodeEnabledChange
                        )
                    }
                    if (shareCodeEnabled) {
                        if (!canCreateShareCode || onCreateShareCode == null) {
                            Text(
                                "Đang kết nối tài khoản thiết bị. Sau khi kết nối xong, bạn có thể tạo hoặc nhập mã.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        OutlinedTextField(
                            value = shareCode,
                            onValueChange = { value ->
                                onShareCodeInputChange(
                                    value.filterNot(Char::isWhitespace)
                                        .uppercase()
                                        .take(32)
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Mã chia sẻ (32 ký tự A-F, 0-9)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii)
                        )
                        Text(
                            if (shareCode.isBlank()) {
                                "Chưa có mã. Tạo mã ngẫu nhiên hoặc nhập mã cũ để khôi phục."
                            } else if (shareCode.length == 32 && shareCode.all {
                                    it.isDigit() || it in 'A'..'F'
                                }) {
                                "Mã hợp lệ. Lưu lại cùng mã cũ sẽ khôi phục quyền chia sẻ trước đó."
                            } else {
                                "Mã cần đúng 32 ký tự từ A-F và 0-9."
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                        onCreateShareCode?.let { createShareCode ->
                            Button(
                                onClick = {
                                scope.launch {
                                    creatingShareCode = true
                                    try {
                                        val code = createShareCode(
                                            shareCode.takeIf { it.isNotBlank() }
                                        )
                                        onShareCodeInputChange(code)
                                        onShareCodeCreated(code)
                                    } catch (error: kotlinx.coroutines.CancellationException) {
                                        throw error
                                    } catch (error: com.google.firebase.FirebaseException) {
                                        onBackupMessage(
                                            "Không tạo được mã chia sẻ: " +
                                                (error.localizedMessage ?: "lỗi Firebase")
                                        )
                                    } catch (error: IllegalArgumentException) {
                                        onBackupMessage(error.message ?: "Mã chia sẻ không hợp lệ.")
                                    } catch (error: IllegalStateException) {
                                        onBackupMessage(
                                            error.message ?: "Không thể tạo mã chia sẻ."
                                        )
                                    } finally {
                                        creatingShareCode = false
                                    }
                                }
                                },
                                enabled = !creatingShareCode &&
                                    (shareCode.isBlank() || (
                                        shareCode.length == 32 &&
                                            shareCode.all { it.isDigit() || it in 'A'..'F' }
                                        )),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (creatingShareCode) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp))
                                    Text("  Đang lưu mã...")
                                } else if (shareCode.isBlank()) {
                                    Icon(Icons.Default.Share, contentDescription = null)
                                    Text("  Tạo mã ngẫu nhiên")
                                } else {
                                    Icon(Icons.Default.Check, contentDescription = null)
                                    Text("  Lưu hoặc khôi phục mã này")
                                }
                            }
                        }
                        Text(
                            "Tắt mã sẽ ngừng yêu cầu mới. Thiết bị đã tham gia còn quyền tới 30 ngày nếu không đồng bộ; mã và dữ liệu cũ không bị xóa.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        if (onLoadShareRequests != null && onDecideShareRequest != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Yêu cầu tham gia (${shareRequests.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(
                                    enabled = !loadingShareRequests,
                                    onClick = {
                                        scope.launch {
                                            loadingShareRequests = true
                                            try {
                                                shareRequests = onLoadShareRequests()
                                            } catch (error: kotlinx.coroutines.CancellationException) {
                                                throw error
                                            } catch (error: com.google.firebase.FirebaseException) {
                                                onBackupMessage(
                                                    "Không tải được yêu cầu: " +
                                                        (error.localizedMessage ?: "lỗi mạng")
                                                )
                                            } catch (error: IllegalStateException) {
                                                onBackupMessage(
                                                    error.message ?: "Không tải được yêu cầu."
                                                )
                                            } finally {
                                                loadingShareRequests = false
                                            }
                                        }
                                    }
                                ) { Text(if (loadingShareRequests) "Đang tải..." else "Làm mới") }
                            }
                            shareRequests.forEach { memberUid ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Thiết bị ${memberUid.takeLast(8)}",
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    TextButton(
                                        enabled = decidingShareRequest != memberUid,
                                        onClick = {
                                            scope.launch {
                                                decidingShareRequest = memberUid
                                                try {
                                                    onDecideShareRequest(memberUid, true)
                                                    shareRequests = shareRequests - memberUid
                                                } catch (error: kotlinx.coroutines.CancellationException) {
                                                    throw error
                                                } catch (error: com.google.firebase.FirebaseException) {
                                                    onBackupMessage(
                                                        "Không duyệt được yêu cầu: " +
                                                            (error.localizedMessage ?: "lỗi Firebase")
                                                    )
                                                } catch (error: IllegalStateException) {
                                                    onBackupMessage(
                                                        error.message ?: "Không duyệt được yêu cầu."
                                                    )
                                                } finally {
                                                    decidingShareRequest = null
                                                }
                                            }
                                        }
                                    ) { Text("Duyệt") }
                                    TextButton(
                                        enabled = decidingShareRequest != memberUid,
                                        onClick = {
                                            scope.launch {
                                                decidingShareRequest = memberUid
                                                try {
                                                    onDecideShareRequest(memberUid, false)
                                                    shareRequests = shareRequests - memberUid
                                                } catch (error: kotlinx.coroutines.CancellationException) {
                                                    throw error
                                                } catch (error: com.google.firebase.FirebaseException) {
                                                    onBackupMessage(
                                                        "Không từ chối được yêu cầu: " +
                                                            (error.localizedMessage ?: "lỗi Firebase")
                                                    )
                                                } catch (error: IllegalStateException) {
                                                    onBackupMessage(
                                                        error.message ?: "Không từ chối được yêu cầu."
                                                    )
                                                } finally {
                                                    decidingShareRequest = null
                                                }
                                            }
                                        }
                                    ) { Text("Từ chối") }
                                }
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { showNearbyShareDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Text("  Chia sẻ xung quanh")
                    }
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Palette, contentDescription = null)
                    Text("  Giao diện", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { appearanceExpanded = !appearanceExpanded }) {
                        Icon(
                            if (appearanceExpanded) Icons.Default.ExpandLess
                            else Icons.Default.ExpandMore,
                            contentDescription = if (appearanceExpanded) {
                                "Thu gọn tùy chọn giao diện"
                            } else {
                                "Mở rộng tùy chọn giao diện"
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val icon = when (mode) {
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                            ThemeMode.CUSTOM -> Icons.Default.Palette
                        }
                        val selected = appearance.mode == mode
                        Surface(
                            modifier = Modifier.weight(1f).height(48.dp)
                                .clickable {
                                    onAppearanceChange(appearance.copy(mode = mode))
                                },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            tonalElevation = if (selected) 2.dp else 0.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    icon,
                                    contentDescription = mode.title,
                                    tint = if (selected) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
                if (appearanceExpanded) {
                    Text("Kiểu thanh tab bên dưới", style = MaterialTheme.typography.titleSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BottomTabStyle.entries.forEach { style ->
                            FilterChip(
                                selected = appearance.bottomTabStyle == style,
                                onClick = {
                                    onAppearanceChange(appearance.copy(bottomTabStyle = style))
                                },
                                label = { Text(style.title) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Hiệu ứng kính mờ", style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Làm trong mờ thanh trên, thanh dưới và nút thêm sản phẩm.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = appearance.glassEffectEnabled,
                            onCheckedChange = { enabled ->
                                onAppearanceChange(appearance.copy(glassEffectEnabled = enabled))
                            }
                        )
                    }
                    Text("Màu giao diện", style = MaterialTheme.typography.titleSmall)
                    ColorSettingButton(
                        title = "Màu nền",
                        color = colorFromHex(backgroundHex) ?: MaterialTheme.colorScheme.background,
                        enabled = appearance.mode == ThemeMode.CUSTOM,
                        onClick = { pickerTarget = "background" }
                    )
                    ColorSettingButton(
                        title = "Màu nút",
                        color = colorFromHex(buttonHex) ?: MaterialTheme.colorScheme.primary,
                        onClick = { pickerTarget = "button" }
                    )
                    ColorSettingButton(
                        title = "Màu phụ",
                        color = MaterialTheme.colorScheme.secondary,
                        onClick = { pickerTarget = "secondary" }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Tùy chỉnh màu chữ và biểu tượng")
                            Text(
                                "Tắt: tự chọn trắng hoặc đen theo nền.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = appearance.customTextEnabled,
                            onCheckedChange = { enabled ->
                                onAppearanceChange(appearance.copy(customTextEnabled = enabled))
                            }
                        )
                    }
                    if (appearance.customTextEnabled) {
                        ColorSettingButton(
                            title = "Màu chữ và biểu tượng",
                            color = colorFromHex(textHex) ?: MaterialTheme.colorScheme.onSurface,
                            onClick = { pickerTarget = "text" }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("#0284C7", "#15803D", "#C2410C", "#BE123C").forEach { hex ->
                            Surface(
                                modifier = Modifier.weight(1f).height(38.dp).clickable {
                                    buttonHex = hex
                                    onAppearanceChange(appearance.copy(buttonHex = hex))
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = colorFromHex(hex) ?: MaterialTheme.colorScheme.primary,
                                tonalElevation = if (buttonHex.equals(hex, ignoreCase = true)) 3.dp else 0.dp
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (buttonHex.equals(hex, ignoreCase = true)) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = "Màu đang chọn",
                                            tint = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Text("  Sao lưu kho", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "ZIP gồm tên hàng, đơn vị, tồn hiện tại, giá nhập/bán và ảnh. " +
                        "Khi trùng tên, bạn chọn bỏ qua, cập nhật tồn nhưng giữ giá hiện tại, hoặc tạo sản phẩm mới."
                )
                Button(
                    onClick = { exportLauncher.launch("QL-kho-sao-luu.zip") },
                    enabled = !working,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Text("  Xuất file ZIP")
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/zip", "application/octet-stream")) },
                    enabled = !working,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Restore, contentDescription = null)
                    Text("  Nhập file ZIP")
                }
                if (working) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            }
            pendingImport?.let { imported ->
                InventoryImportDialog(
                    imported = imported,
                    existingProducts = products,
                    onDismiss = {
                        try {
                            InventoryBackup.deleteImportedImages(imported)
                        } catch (failure: IOException) {
                            onBackupMessage(
                                "Không dọn được ảnh tạm: ${failure.localizedMessage ?: "lỗi tệp"}"
                            )
                        }
                        pendingImport = null
                    },
                    onConfirm = { decisions ->
                        onImport(decisions)
                        pendingImport = null
                    }
                )
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Text("  Cài đặt nâng cao", style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    "Xóa toàn bộ sản phẩm khỏi danh sách kho. Lịch sử xuất nhập và đơn hàng cũ vẫn được giữ."
                )
                OutlinedButton(
                    onClick = { confirmDeleteAll = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Text("  Xóa toàn bộ sản phẩm")
                }
            }
        }
        }
    }
    if (showNearbyShareDialog) {
        AlertDialog(
            onDismissRequest = { showNearbyShareDialog = false },
            title = { DialogTitleWithClose("Chia sẻ xung quanh") { showNearbyShareDialog = false } },
            text = {
                Text(
                    "Phát sẽ mở bảng chia sẻ Android, trong đó có Quick Share nếu thiết bị hỗ trợ. " +
                        "Nhận mã bằng cách chọn ứng dụng này từ Quick Share hoặc nhập/dán mã. " +
                        "Việc chuyển mã có thể ở gần; đồng bộ dữ liệu kho vẫn cần Internet."
                )
            },
            confirmButton = {
                TextButton(
                    enabled = shareCodeEnabled &&
                        shareCode.isNotBlank() &&
                        shareCode == savedShareCode,
                    onClick = {
                        showNearbyShareDialog = false
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "QL-kho share code: $shareCode")
                        }
                        try {
                            context.startActivity(
                                Intent.createChooser(sendIntent, "Phát mã kho")
                            )
                        } catch (error: android.content.ActivityNotFoundException) {
                            onBackupMessage("Không tìm thấy ứng dụng chia sẻ trên thiết bị.")
                        } catch (error: SecurityException) {
                            onBackupMessage("Không thể mở bảng chia sẻ trên thiết bị.")
                        }
                    }
                ) { Text("Phát mã") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNearbyShareDialog = false
                        onReceiveNearbyShareCode()
                    }
                ) { Text("Nhận mã") }
            }
        )
    }
    pickerTarget?.let { target ->
        ColorPickerDialog(
            initialHex = when (target) {
                "background" -> backgroundHex
                "button" -> buttonHex
                "secondary" -> secondaryHex
                else -> textHex
            },
            title = when (target) {
                "background" -> "Chọn màu nền"
                "button" -> "Chọn màu nút"
                "secondary" -> "Chọn màu phụ"
                else -> "Chọn màu chữ và biểu tượng"
            },
            onDismiss = { pickerTarget = null },
            onApply = { colorHex ->
                when (target) {
                    "background" -> {
                        backgroundHex = colorHex
                        onAppearanceChange(appearance.copy(mode = ThemeMode.CUSTOM, backgroundHex = colorHex))
                    }
                    "button" -> {
                        buttonHex = colorHex
                        onAppearanceChange(appearance.copy(buttonHex = colorHex))
                    }
                    "secondary" -> {
                        secondaryHex = colorHex
                        onAppearanceChange(appearance.copy(secondaryHex = colorHex))
                    }
                    else -> {
                        textHex = colorHex
                        onAppearanceChange(appearance.copy(textHex = colorHex))
                    }
                }
                pickerTarget = null
            }
        )
    }
    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title = { DialogTitleWithClose("Xóa toàn bộ sản phẩm?") { confirmDeleteAll = false } },
            text = {
                Text(
                    "Bạn có chắc muốn xóa hẳn tất cả sản phẩm và ảnh khỏi danh sách? " +
                        "Lịch sử đơn hàng và xuất nhập kho vẫn được giữ."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteAllProducts()
                        confirmDeleteAll = false
                    }
                ) { Text("Có, xóa") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteAll = false }) { Text("Không") }
            }
        )
    }
}

@Composable
private fun ProductDialog(
    vm: KhoViewModel,
    product: Product?,
    initialImageUri: Uri? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by remember(product?.id) { mutableStateOf(product?.name.orEmpty()) }
    var unitType by remember(product?.id) { mutableStateOf(UnitType.PIECE) }
    var openingStock by remember(product?.id) { mutableStateOf("0") }
    var buyPrice by remember(product?.id) { mutableStateOf("0") }
    var sellPrice by remember(product?.id) { mutableStateOf("0") }
    var imagePath by remember(product?.id) { mutableStateOf(product?.imagePath) }
    var additionalImagePaths by remember(product?.id) {
        mutableStateOf(product?.additionalImagePaths.orEmpty())
    }
    var error by remember { mutableStateOf<String?>(null) }
    var savingImage by remember { mutableStateOf(false) }
    var showGallery by remember { mutableStateOf(false) }
    var cropUri by remember { mutableStateOf<Uri?>(null) }
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var cropIsAdditional by remember { mutableStateOf(false) }
    var pendingAdditionalUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var imageSourceSizeBytes by remember { mutableStateOf<Long?>(null) }
    var cameraCaptureFile by remember { mutableStateOf<File?>(null) }
    var cameraCaptureUri by remember { mutableStateOf<Uri?>(null) }
    var cameraCaptureIsAdditional by remember { mutableStateOf(false) }
    var createdImagePaths by remember(product?.id) { mutableStateOf(emptyList<String>()) }

    fun cleanupNewImages(retainedPaths: Set<String>) {
        val unusedPaths = createdImagePaths.filterNot { it in retainedPaths }
        try {
            InventoryBackup.deleteImages(unusedPaths)
            createdImagePaths = createdImagePaths.filter { it in retainedPaths }
        } catch (failure: IOException) {
            vm.message = "Không dọn được ảnh chưa lưu: ${failure.localizedMessage ?: "lỗi tệp"}"
        }
    }

    fun dismissProductDialog() {
        cleanupNewImages(emptySet())
        onDismiss()
    }

    fun saveImage(bitmap: Bitmap, asAdditional: Boolean, onSaved: () -> Unit = {}) {
        val sourceSize = imageSourceSizeBytes
        scope.launch {
            savingImage = true
            try {
                val savedPath = withContext(Dispatchers.IO) {
                    persistProductImage(context, null, bitmap, sourceSize)
                }
                if (asAdditional) {
                    additionalImagePaths = additionalImagePaths + savedPath
                } else {
                    imagePath = savedPath
                }
                createdImagePaths = createdImagePaths + savedPath
                error = null
                onSaved()
            } catch (failure: IOException) {
                error = "Không lưu được ảnh: ${failure.localizedMessage ?: "lỗi tệp"}"
            } finally {
                savingImage = false
            }
        }
    }

    fun prepareAdditionalImages(uris: List<Uri>) {
        val remainingSlots = 10 - additionalImagePaths.size
        if (uris.size > remainingSlots) {
            error = "Mỗi sản phẩm có thể có tối đa 10 ảnh phụ."
            return
        }
        val first = uris.firstOrNull() ?: return
        pendingAdditionalUris = uris.drop(1)
        cropIsAdditional = true
        cropUri = first
        cropBitmap = null
        imageSourceSizeBytes = queryImageSize(context, first)
    }

    LaunchedEffect(initialImageUri) {
        if (initialImageUri != null) {
            cropIsAdditional = false
            cropUri = initialImageUri
            cropBitmap = null
            imageSourceSizeBytes = queryImageSize(context, initialImageUri)
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (hasGalleryPermission(context)) {
            showGallery = true
        } else {
            error = if (results.isEmpty()) "Không thể mở quyền truy cập ảnh."
            else "Cần cho phép truy cập ảnh để chọn ảnh trong thư viện."
        }
    }
    val cameraPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { captured ->
        val captureFile = cameraCaptureFile
        if (captured && captureFile?.isFile == true) {
            cropUri = cameraCaptureUri
            cropIsAdditional = cameraCaptureIsAdditional
            imageSourceSizeBytes = captureFile.length()
            cropBitmap = null
        } else {
            captureFile?.delete()
            cameraCaptureFile = null
            cameraCaptureUri = null
            if (!captured) error = "Không chụp được ảnh."
        }
    }
    val additionalGalleryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        prepareAdditionalImages(uris)
    }
    fun captureImage(asAdditional: Boolean = false) {
        if (asAdditional && additionalImagePaths.size >= 10) {
            error = "Mỗi sản phẩm có thể có tối đa 10 ảnh phụ."
            return
        }
        try {
            val directory = File(context.cacheDir, "product-captures")
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Không tạo được thư mục ảnh tạm.")
            }
            val file = File.createTempFile("capture_", ".jpg", directory)
            cameraCaptureFile = file
            cameraCaptureIsAdditional = asAdditional
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            cameraCaptureUri = uri
            cameraPicker.launch(uri)
        } catch (failure: IOException) {
            error = "Không thể mở máy ảnh: ${failure.localizedMessage ?: "lỗi tệp"}"
        } catch (failure: IllegalArgumentException) {
            error = "Không thể tạo địa chỉ ảnh tạm."
        }
    }
    fun chooseGalleryImage() {
        if (hasGalleryPermission(context)) {
            showGallery = true
        } else {
            galleryPermissionLauncher.launch(galleryPermissionNames())
        }
    }

    AlertDialog(
        onDismissRequest = ::dismissProductDialog,
        title = {
            DialogTitleWithClose(
                if (product == null) "Thêm sản phẩm" else "Sửa sản phẩm",
                ::dismissProductDialog
            )
        },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProductImage(imagePath, Modifier.size(76.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        OutlinedButton(
                            onClick = { chooseGalleryImage() },
                            enabled = !savingImage
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            Spacer(Modifier.size(8.dp))
                            Text("Chọn ảnh")
                        }
                        OutlinedButton(
                            onClick = { captureImage() },
                            enabled = !savingImage
                        ) {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null)
                            Text("Chụp ảnh")
                        }
                    }
                }
                Text(
                    "Ảnh phụ (${additionalImagePaths.size}/10)",
                    style = MaterialTheme.typography.titleSmall
                )
                if (additionalImagePaths.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(additionalImagePaths) { path ->
                            Box(Modifier.size(76.dp)) {
                                ProductImage(path, Modifier.fillMaxSize())
                                IconButton(
                                    onClick = {
                                        additionalImagePaths =
                                            additionalImagePaths.filterNot { it == path }
                                    },
                                    modifier = Modifier.align(Alignment.TopEnd)
                                        .size(28.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.6f),
                                            CircleShape
                                        )
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Xóa ảnh phụ",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { additionalGalleryPicker.launch("image/*") },
                        enabled = !savingImage && additionalImagePaths.size < 10
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Chọn nhiều ảnh")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { captureImage(asAdditional = true) },
                        enabled = !savingImage && additionalImagePaths.size < 10
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text("Thêm ảnh chụp")
                    }
                }
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên sản phẩm") },
                    singleLine = true
                )
                if (product == null) {
                    Text("Đơn vị quản lý")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = unitType == UnitType.PIECE,
                            onClick = { unitType = UnitType.PIECE }
                        )
                        Text("Cái")
                        RadioButton(
                            selected = unitType == UnitType.WEIGHT,
                            onClick = { unitType = UnitType.WEIGHT }
                        )
                        Text("Kg")
                    }
                    OutlinedTextField(
                        value = openingStock,
                        onValueChange = { openingStock = it },
                        label = {
                            Text("Tồn đầu kỳ (${if (unitType == UnitType.WEIGHT) "kg" else "cái"})")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = if (unitType == UnitType.WEIGHT) {
                                KeyboardType.Decimal
                            } else {
                                KeyboardType.Number
                            }
                        )
                    )
                    CurrencyTextField(
                        value = buyPrice,
                        onValueChange = { buyPrice = it },
                        label = "Giá nhập / ${if (unitType == UnitType.WEIGHT) "kg" else "cái"}"
                    )
                    CurrencyTextField(
                        value = sellPrice,
                        onValueChange = { sellPrice = it },
                        label = "Giá bán / ${if (unitType == UnitType.WEIGHT) "kg" else "cái"}"
                    )
                }
                if (savingImage) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Text(" Đang lưu ảnh…")
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (savingImage) return@TextButton
                if (product != null) {
                    error = when {
                        name.isBlank() -> "Nhập tên sản phẩm."
                        vm.products.value.any {
                            it.id != product.id &&
                                normalizeProductName(it.name).equals(
                                    normalizeProductName(name),
                                    ignoreCase = true
                                )
                        } -> "Tên này đang được dùng cho sản phẩm khác."
                        else -> null
                    }
                    if (error == null) {
                        cleanupNewImages(
                            setOfNotNull(imagePath) + additionalImagePaths
                        )
                        vm.updateProduct(
                            product.id,
                            name,
                            imagePath,
                            additionalImagePaths
                        )
                        onDismiss()
                    }
                } else {
                    val stock = openingStock.toDoubleOrNull()
                    val buy = parseCurrencyInput(buyPrice)
                    val sell = parseCurrencyInput(sellPrice)
                    val duplicate = vm.products.value.firstOrNull {
                        normalizeProductName(it.name).equals(
                            normalizeProductName(name),
                            ignoreCase = true
                        )
                    }
                    error = when {
                        name.isBlank() -> "Nhập tên sản phẩm."
                        stock == null || !stock.isFinite() || stock < 0.0 ->
                            "Tồn đầu kỳ không hợp lệ."
                        unitType == UnitType.PIECE && stock % 1.0 != 0.0 ->
                            "Số lượng Cái phải là số nguyên."
                        buy == null || buy < 0L -> "Giá nhập không hợp lệ."
                        sell == null || sell < 0L -> "Giá bán không hợp lệ."
                        else -> null
                    }
                    if (error == null) {
                        if (duplicate == null) {
                            cleanupNewImages(
                                setOfNotNull(imagePath) + additionalImagePaths
                            )
                            vm.saveProduct(
                                name,
                                unitType,
                                openingStock,
                                buyPrice,
                                sellPrice,
                                imagePath,
                                additionalImagePaths
                            )
                        } else {
                            val matchingBranch = duplicate.branches.firstOrNull {
                                it.unitType == unitType
                            }
                            when {
                                matchingBranch == null -> {
                                    error = "Sản phẩm đã có nhưng khác đơn vị. Mở sản phẩm đó để thêm đơn vị."
                                }
                                stock == 0.0 -> {
                                    error = "Sản phẩm đã tồn tại. Không tạo bản trùng."
                                }
                                else -> {
                                    if (
                                        imagePath != null ||
                                        additionalImagePaths.isNotEmpty()
                                    ) {
                                        cleanupNewImages(
                                            setOfNotNull(imagePath) + additionalImagePaths
                                        )
                                        vm.updateProduct(
                                            duplicate.id,
                                            duplicate.name,
                                            imagePath ?: duplicate.imagePath,
                                            (
                                                duplicate.additionalImagePaths +
                                                    additionalImagePaths
                                                ).distinct()
                                        )
                                    }
                                    vm.mergeDuplicateProduct(
                                        duplicate.name,
                                        matchingBranch.id,
                                        openingStock,
                                        buyPrice
                                    )
                                }
                            }
                        }
                    }
                }
            }) { Text(if (product == null) "Lưu sản phẩm" else "Lưu thay đổi") }
        },
    )
    if (showGallery) {
        AppGalleryPicker(
            onDismiss = { showGallery = false },
            onSelect = { uri ->
                showGallery = false
                cropIsAdditional = false
                cropUri = uri
                cropBitmap = null
                imageSourceSizeBytes = queryImageSize(context, uri)
            }
        )
    }
    if (cropUri != null || cropBitmap != null) {
        PhotoCropDialog(
            sourceUri = cropUri,
            sourceBitmap = cropBitmap,
            onDismiss = {
                cropUri = null
                cropBitmap = null
                pendingAdditionalUris = emptyList()
                cameraCaptureFile?.delete()
                cameraCaptureFile = null
                cameraCaptureUri = null
            },
            onCropComplete = { bitmap ->
                cropUri = null
                cropBitmap = null
                val wasAdditional = cropIsAdditional
                val nextAdditionalUri = pendingAdditionalUris.firstOrNull()
                pendingAdditionalUris = pendingAdditionalUris.drop(1)
                saveImage(bitmap, wasAdditional) {
                    if (wasAdditional && nextAdditionalUri != null) {
                        prepareAdditionalImages(
                            listOf(nextAdditionalUri) + pendingAdditionalUris
                        )
                    }
                }
                cameraCaptureFile?.delete()
                cameraCaptureFile = null
                cameraCaptureUri = null
                imageSourceSizeBytes = null
            },
            onCropError = { message ->
                error = "Không cắt được ảnh: $message"
            }
        )
    }
}

@Composable
private fun ProductImage(path: String?, modifier: Modifier = Modifier) {
    val bitmap = remember(path) { path?.let(::decodeProductThumbnail) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Ảnh sản phẩm",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(Icons.Default.Inventory2, contentDescription = null)
        }
    }
}

private fun galleryPermissionNames(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
    )
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
        arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

private fun hasGalleryPermission(context: Context): Boolean = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE ->
        context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) ==
            PackageManager.PERMISSION_GRANTED ||
            context.checkSelfPermission(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) ==
            PackageManager.PERMISSION_GRANTED
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
        context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) ==
            PackageManager.PERMISSION_GRANTED
    else -> context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
        PackageManager.PERMISSION_GRANTED
}

private suspend fun queryGalleryImages(context: Context): List<GalleryImage> =
    withContext(Dispatchers.IO) {
        val images = mutableListOf<GalleryImage>()
        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED
        )
        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn).orEmpty().ifBlank { "Ảnh" }
                images += GalleryImage(
                    uri = ContentUris.withAppendedId(collection, id),
                    name = name,
                    dateAdded = cursor.getLong(dateColumn)
                )
            }
        } ?: throw IOException("Không truy cập được thư viện ảnh.")
        images
    }

@Composable
private fun AppGalleryPicker(
    onDismiss: () -> Unit,
    onSelect: (Uri) -> Unit
) {
    val context = LocalContext.current
    var images by remember { mutableStateOf<List<GalleryImage>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        try {
            images = queryGalleryImages(context)
        } catch (failure: SecurityException) {
            error = "Không có quyền xem ảnh. Cho phép quyền ảnh rồi thử lại."
        } catch (failure: IOException) {
            error = failure.localizedMessage ?: "Không tải được thư viện ảnh."
        } finally {
            loading = false
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.96f).fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(Modifier.fillMaxSize().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Chọn ảnh", style = MaterialTheme.typography.titleLarge)
                        Text("${images.size} ảnh · Mới nhất trước")
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng thư viện ảnh")
                    }
                }
                when {
                    loading -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                    error != null -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { Text(error!!, color = MaterialTheme.colorScheme.error) }
                    images.isEmpty() -> Box(
                        Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) { EmptyState("Chưa tìm thấy ảnh trong thư viện trên thiết bị.") }
                    else -> LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(images, key = { it.uri.toString() }) { image ->
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelect(image.uri) }
                            ) {
                                GalleryThumbnail(image.uri, Modifier.fillMaxSize())
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryThumbnail(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, uri) {
        value = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(320, 320), null)
                } else {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, bounds)
                    }
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) null
                    else {
                        var sample = 1
                        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 320) {
                            sample *= 2
                        }
                        context.contentResolver.openInputStream(uri)?.use {
                            BitmapFactory.decodeStream(
                                it,
                                null,
                                BitmapFactory.Options().apply { inSampleSize = sample }
                            )
                        }
                    }
                }
            } catch (_: IOException) {
                null
            } catch (_: SecurityException) {
                null
            }
        }
    }
    if (bitmap == null) {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    } else {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Ảnh trong thư viện",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

private fun decodeProductThumbnail(path: String): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > 512) {
        sampleSize *= 2
    }
    return BitmapFactory.decodeFile(
        path,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    )
}

private fun persistProductImage(
    context: Context,
    uri: Uri?,
    bitmap: Bitmap?,
    sourceSizeBytes: Long? = null
): String {
    require((uri == null) != (bitmap == null)) { "Chọn một ảnh để lưu." }
    val imageDirectory = File(context.filesDir, "product-images")
    if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
        throw IOException("Không tạo được thư mục ảnh.")
    }
    val destination = File(imageDirectory, "${java.util.UUID.randomUUID()}.jpg")
    try {
        var sourceBitmap = bitmap ?: decodeProductImage(context, checkNotNull(uri))
        try {
            val inputSize = sourceSizeBytes ?: uri?.let { queryImageSize(context, it) }
            val maxDimension = when {
                inputSize != null && inputSize > 10L * 1024 * 1024 -> 854
                inputSize != null && inputSize > 5L * 1024 * 1024 -> 1280
                else -> Int.MAX_VALUE
            }
            if (maxOf(sourceBitmap.width, sourceBitmap.height) > maxDimension) {
                val scale = maxDimension.toFloat() /
                    maxOf(sourceBitmap.width, sourceBitmap.height)
                val resized = Bitmap.createScaledBitmap(
                    sourceBitmap,
                    (sourceBitmap.width * scale).toInt().coerceAtLeast(1),
                    (sourceBitmap.height * scale).toInt().coerceAtLeast(1),
                    true
                )
                if (resized !== sourceBitmap) sourceBitmap.recycle()
                sourceBitmap = resized
            }
            FileOutputStream(destination).use { output ->
                if (!sourceBitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)) {
                    throw IOException("Không mã hóa được ảnh.")
                }
            }
        } finally {
            sourceBitmap.recycle()
        }
        return destination.absolutePath
    } catch (failure: IOException) {
        destination.delete()
        throw failure
    } catch (failure: SecurityException) {
        destination.delete()
        throw IOException("Không có quyền đọc ảnh đã chọn.", failure)
    }
}

private fun queryImageSize(context: Context, uri: Uri): Long? =
    try {
        context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            descriptor.length.takeIf { it >= 0L }
        }
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        null
    }

private fun decodeProductImage(context: Context, uri: Uri): Bitmap {
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: throw IOException("Không đọc được ảnh đã chọn.")
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IOException("Tệp ảnh không hợp lệ.")
    }
    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > 1280) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        ?: throw IOException("Không giải mã được ảnh.")
}

@Composable
private fun EmptyState(message: String) {
    GlassCard(Modifier.fillMaxWidth()) { Text(message) }
}

@Composable
private fun CurrencyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChange(formatCurrencyInput(it)) },
        modifier = modifier,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

private fun money(value: Long): String =
    NumberFormat.getNumberInstance(Locale("vi", "VN")).format(value) + " ₫"

private fun normalizeProductName(name: String): String =
    name.trim().replace(Regex("\\s+"), " ")

private fun quantity(value: Double): String =
    NumberFormat.getNumberInstance(Locale("vi", "VN")).format(value)

private fun quantity(value: Double, unitType: UnitType): String =
    if (unitType == UnitType.WEIGHT && value < 1.0) {
        "${NumberFormat.getNumberInstance(Locale("vi", "VN")).format(value * 1000.0)} g"
    } else {
        "${quantity(value)} ${if (unitType == UnitType.WEIGHT) "kg" else ""}".trim()
    }

private fun isLowStock(branch: UnitBranch): Boolean =
    branch.stock < 0.0 || if (branch.unitType == UnitType.WEIGHT) {
        branch.stock < branch.lowStockThreshold
    } else {
        branch.stock <= branch.lowStockThreshold
    }

private fun dateTime(value: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN")).format(Date(value))

private fun orderStatus(value: String): String = when (value) {
    "COMPLETED" -> "Đã thanh toán"
    "PARTIALLY_PAID" -> "Thanh toán một phần"
    "CANCELLED" -> "Đã hủy"
    else -> "Chưa thanh toán"
}
