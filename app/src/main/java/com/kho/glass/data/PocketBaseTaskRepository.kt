package com.kho.glass.data

import android.content.Context
import java.io.IOException
import org.json.JSONObject

data class PocketBaseTask(
    val id: String,
    val title: String
)

class PocketBaseTaskRepository(private val context: Context) {
    suspend fun readTasks(): List<PocketBaseTask> {
        val tasks = mutableListOf<PocketBaseTask>()
        var page = 1
        var totalPages: Int
        do {
            val response = PocketBaseAuth.request(
                context,
                "/api/collections/task/records?page=$page&sort=-created&perPage=200"
            )
            val items = response.optJSONArray("items")
                ?: throw IOException("PocketBase trả về danh sách task không hợp lệ.")
            for (index in 0 until items.length()) {
                val record = items.optJSONObject(index)
                    ?: throw IOException("PocketBase trả về task không hợp lệ.")
                tasks += record.toTask()
            }
            totalPages = response.optInt("totalPages", page).coerceAtLeast(page)
            page++
        } while (page <= totalPages)
        return tasks
    }

    suspend fun addTask(title: String): PocketBaseTask {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Tên công việc không được để trống." }
        val record = PocketBaseAuth.request(
            context,
            "/api/collections/task/records",
            "POST",
            JSONObject().put("title", cleanTitle)
        )
        return record.toTask()
    }

    suspend fun updateTask(id: String, title: String): PocketBaseTask {
        val cleanTitle = title.trim()
        require(id.isNotBlank()) { "Công việc không hợp lệ." }
        require(cleanTitle.isNotEmpty()) { "Tên công việc không được để trống." }
        check(PocketBaseAuth.session(context) != null) {
            "Hãy đăng nhập PocketBase trước."
        }
        val record = PocketBaseAuth.request(
            context,
            "/api/collections/task/records/$id",
            "PATCH",
            JSONObject().put("title", cleanTitle)
        )
        return record.toTask()
    }

    private fun JSONObject.toTask(): PocketBaseTask {
        val id = optString("id").takeIf(String::isNotBlank)
            ?: throw IOException("PocketBase trả về task không có mã hợp lệ.")
        if (!has("title") || isNull("title")) {
            throw IOException("PocketBase trả về task không có tiêu đề hợp lệ.")
        }
        val title = try {
            getString("title")
        } catch (error: org.json.JSONException) {
            throw IOException("PocketBase trả về tiêu đề task không hợp lệ.", error)
        }
        return PocketBaseTask(id, title)
    }
}
