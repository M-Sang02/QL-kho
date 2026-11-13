package com.kho.glass.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Locale
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class InventoryBackupUnit(
    val name: String,
    val unitType: UnitType,
    val quantity: Double,
    val buyPrice: Long,
    val sellPrice: Long
)

data class InventoryBackupProduct(
    val name: String,
    val imagePath: String?,
    val units: List<InventoryBackupUnit>,
    val additionalImagePaths: List<String> = emptyList()
)

enum class DuplicateImportAction {
    SKIP,
    KEEP_CURRENT_PRICES,
    CREATE_NEW
}

data class InventoryImportDecision(
    val product: InventoryBackupProduct,
    val action: DuplicateImportAction,
    val newName: String? = null
)

object InventoryBackup {
    private const val MANIFEST = "inventory.json"
    private const val MAX_ENTRY_BYTES = 25 * 1024 * 1024
    private const val MAX_TOTAL_BYTES = 150 * 1024 * 1024
    private const val MAX_ENTRIES = 5_000

    fun write(context: Context, destination: Uri, products: List<Product>) {
        val resolver = context.contentResolver
        val output = resolver.openOutputStream(destination, "w")
            ?: throw IOException("Không mở được tệp ZIP để ghi.")
        ZipOutputStream(output.buffered()).use { zip ->
            val manifest = JSONObject().put("formatVersion", 2)
            val productArray = JSONArray()
            products.forEachIndexed { index, product ->
                val imageEntry = product.imagePath?.takeIf { File(it).isFile }?.let {
                    "images/$index-cover.jpg"
                }
                val additionalImageEntries = JSONArray()
                val existingAdditionalImages = product.additionalImagePaths
                    .filter { File(it).isFile }
                existingAdditionalImages.forEachIndexed { imageIndex, _ ->
                    additionalImageEntries.put("images/$index-extra-$imageIndex.jpg")
                }
                val units = JSONArray()
                product.branches.forEach { branch ->
                    units.put(
                        JSONObject()
                            .put("name", branch.name)
                            .put("unitType", branch.unitType.name)
                            .put("quantity", branch.stock)
                            .put("buyPrice", branch.buyPrice)
                            .put("sellPrice", branch.sellPrice)
                    )
                }
                productArray.put(
                    JSONObject()
                        .put("name", product.name)
                        .put("imageEntry", imageEntry ?: JSONObject.NULL)
                        .put("additionalImageEntries", additionalImageEntries)
                        .put("units", units)
                )
                imageEntry?.let { entry ->
                    zip.putNextEntry(ZipEntry(entry))
                    File(checkNotNull(product.imagePath))
                        .inputStream().buffered().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
                existingAdditionalImages.forEachIndexed { imageIndex, path ->
                    zip.putNextEntry(ZipEntry("images/$index-extra-$imageIndex.jpg"))
                    File(path).inputStream().buffered().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
            manifest.put("products", productArray)
            zip.putNextEntry(ZipEntry(MANIFEST))
            zip.write(manifest.toString().toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    fun read(context: Context, source: Uri): List<InventoryBackupProduct> {
        var manifestBytes: ByteArray? = null
        val imageBytes = mutableMapOf<String, ByteArray>()
        var totalBytes = 0
        var entryCount = 0
        val input = context.contentResolver.openInputStream(source)
            ?: throw IOException("Không mở được tệp ZIP.")
        ZipInputStream(input.buffered()).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entryCount++
                require(entryCount <= MAX_ENTRIES) { "Tệp ZIP có quá nhiều mục." }
                if (!entry.isDirectory) {
                    require(!entry.name.startsWith("/") &&
                        !entry.name.contains("\\") &&
                        entry.name.split('/').none { it == ".." }
                    ) {
                        "Tệp ZIP có đường dẫn không hợp lệ."
                    }
                    val contents = zip.readLimited()
                    totalBytes += contents.size
                    require(totalBytes <= MAX_TOTAL_BYTES) {
                        "Tệp sao lưu vượt quá giới hạn dung lượng."
                    }
                    when {
                        entry.name == MANIFEST -> {
                            require(manifestBytes == null) { "ZIP có nhiều tệp inventory.json." }
                            manifestBytes = contents
                        }
                        entry.name.startsWith("images/") &&
                            entry.name.removePrefix("images/").isNotBlank() &&
                            !entry.name.removePrefix("images/").contains('/') -> {
                            require(imageBytes.put(entry.name, contents) == null) {
                                "ZIP có tên ảnh bị lặp."
                            }
                        }
                        else -> throw IOException("ZIP chứa tệp không được hỗ trợ.")
                    }
                }
                zip.closeEntry()
            }
        }
        val manifestContent = manifestBytes ?: throw IOException("ZIP không có tệp inventory.json.")

        val manifest = try {
            JSONObject(String(manifestContent, Charsets.UTF_8))
        } catch (failure: JSONException) {
            throw IOException("Tệp sao lưu không đúng định dạng.", failure)
        }
        val formatVersion = manifest.optInt("formatVersion")
        require(formatVersion in 1..2) { "Phiên bản tệp sao lưu không được hỗ trợ." }
        val products = manifest.optJSONArray("products")
            ?: throw IOException("Tệp sao lưu thiếu danh sách sản phẩm.")
        val imageDirectory = File(context.filesDir, "product-images")
        if (!imageDirectory.exists() && !imageDirectory.mkdirs()) {
            throw IOException("Không tạo được thư mục ảnh.")
        }
        val imported = mutableListOf<InventoryBackupProduct>()
        val createdImagePaths = mutableListOf<String>()
        try {
            for (index in 0 until products.length()) {
                val product = products.getJSONObject(index)
                val name = product.getString("name").trim()
                require(name.isNotBlank()) { "Tệp sao lưu có sản phẩm chưa đặt tên." }
                val unitsJson = product.getJSONArray("units")
                require(unitsJson.length() > 0) { "Sản phẩm “$name” không có đơn vị." }
                val unitNames = mutableSetOf<String>()
                val units = (0 until unitsJson.length()).map { unitIndex ->
                    val unit = unitsJson.getJSONObject(unitIndex)
                    val quantity = unit.getDouble("quantity")
                    val buy = unit.getLong("buyPrice")
                    val sell = unit.getLong("sellPrice")
                    val unitName = unit.getString("name").trim()
                    require(unitName.isNotBlank()) { "Sản phẩm “$name” có đơn vị chưa đặt tên." }
                    require(unitNames.add(unitName.lowercase(Locale.ROOT))) {
                        "Sản phẩm “$name” có đơn vị bị lặp."
                    }
                    val unitType = UnitType.valueOf(unit.getString("unitType"))
                    require(quantity.isFinite() && quantity >= 0.0 && buy >= 0L && sell >= 0L) {
                        "Dữ liệu tồn kho/giá không hợp lệ cho “$name”."
                    }
                    require(unitType != UnitType.PIECE || quantity % 1.0 == 0.0) {
                        "Số lượng Cái không hợp lệ cho “$name”."
                    }
                    InventoryBackupUnit(
                        name = unitName,
                        unitType = unitType,
                        quantity = quantity,
                        buyPrice = buy,
                        sellPrice = sell
                    )
                }
                val imageEntry = product.optString("imageEntry")
                    .takeUnless { it.isBlank() || it == "null" }
                val imagePath = imageEntry?.let { entry ->
                    require(entry.startsWith("images/") &&
                        !entry.removePrefix("images/").contains('/')
                    ) { "Đường dẫn ảnh không hợp lệ cho “$name”." }
                    val bytes = imageBytes[entry]
                        ?: throw IOException("Thiếu ảnh sản phẩm “$name” trong ZIP.")
                    val file = File(imageDirectory, "${UUID.randomUUID()}.jpg")
                    createdImagePaths += file.absolutePath
                    FileOutputStream(file).use { it.write(bytes) }
                    file.absolutePath
                }
                val additionalImagePaths = mutableListOf<String>()
                val additionalImageEntries = product.optJSONArray("additionalImageEntries")
                if (additionalImageEntries != null) {
                    require(additionalImageEntries.length() <= 10) {
                        "Sản phẩm “$name” có quá 10 ảnh phụ."
                    }
                    for (imageIndex in 0 until additionalImageEntries.length()) {
                        val entry = additionalImageEntries.getString(imageIndex)
                        require(entry.startsWith("images/") &&
                            !entry.removePrefix("images/").contains('/')
                        ) { "Đường dẫn ảnh phụ không hợp lệ cho “$name”." }
                        val bytes = imageBytes[entry]
                            ?: throw IOException("Thiếu ảnh phụ của sản phẩm “$name” trong ZIP.")
                        val file = File(imageDirectory, "${UUID.randomUUID()}.jpg")
                        createdImagePaths += file.absolutePath
                        FileOutputStream(file).use { it.write(bytes) }
                        additionalImagePaths += file.absolutePath
                    }
                }
                imported += InventoryBackupProduct(name, imagePath, units, additionalImagePaths)
            }
        } catch (failure: Exception) {
            deleteImages(createdImagePaths)
            throw failure
        }
        return imported
    }

    fun deleteImportedImages(products: List<InventoryBackupProduct>) {
        deleteImages(
            products.flatMap { listOfNotNull(it.imagePath) + it.additionalImagePaths }
        )
    }

    fun deleteImages(paths: List<String?>) {
        paths.filterNotNull().distinct().forEach { path ->
            val image = File(path)
            if (image.parentFile?.name == "product-images" && image.exists() && !image.delete()) {
                throw IOException("Không thể dọn ảnh tạm “${image.name}”.")
            }
        }
    }

    private fun ZipInputStream.readLimited(): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            if (output.size() + read > MAX_ENTRY_BYTES) {
                throw IOException("Một tệp bên trong ZIP vượt quá giới hạn dung lượng.")
            }
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }
}
