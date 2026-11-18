package com.kho.glass.data

import android.content.Context
import android.util.Log
import java.io.IOException
import kotlinx.coroutines.CancellationException
import org.json.JSONObject

data class PocketBaseTask(
    val id: String,
    val title: String
)

class PocketBaseTaskRepository(private val context: Context) {
    suspend fun readTasks(): List<PocketBaseTask> {
        return logOperation("read") {
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
            tasks
        }.also { tasks ->
            Log.i(TAG, "Read ${tasks.size} task record(s).")
        }
    }

    suspend fun addTask(title: String): PocketBaseTask = logOperation("create") {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotEmpty()) { "Tên công việc không được để trống." }
        val record = PocketBaseAuth.request(
            context,
            "/api/collections/task/records",
            "POST",
            JSONObject().put("title", cleanTitle)
        )
        record.toTask()
    }

    suspend fun updateTask(id: String, title: String): PocketBaseTask =
        logOperation("update") {
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
            record.toTask()
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

    private suspend fun <T> logOperation(
        operation: String,
        block: suspend () -> T
    ): T {
        Log.i(TAG, "Starting task $operation.")
        return try {
            block().also { Log.i(TAG, "Task $operation completed.") }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "Task $operation failed: ${error.localizedMessage}", error)
            throw error
        }
    }

    private companion object {
        const val TAG = "PocketBaseTaskSync"
    }
}
