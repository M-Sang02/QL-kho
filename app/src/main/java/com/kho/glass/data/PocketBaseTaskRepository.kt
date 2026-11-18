package com.kho.glass.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class PocketBaseTask(
    val id: String,
    val title: String
)

class PocketBaseTaskRepository(private val context: Context) {
    suspend fun readTasks(): List<PocketBaseTask> {
        val response = PocketBaseAuth.request(
            context,
            "/api/collections/task/records?sort=-created&perPage=200"
        )
        val items = response.optJSONArray("items") ?: JSONArray()
        return buildList(items.length()) {
            for (index in 0 until items.length()) {
                val record = items.optJSONObject(index) ?: continue
                add(PocketBaseTask(record.getString("id"), record.getString("title")))
            }
        }
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
        return PocketBaseTask(record.getString("id"), record.getString("title"))
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
        return PocketBaseTask(record.getString("id"), record.getString("title"))
    }
}
