package com.kho.glass.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.json.JSONException
import java.io.IOException
import java.net.MalformedURLException
import java.net.HttpURLConnection
import java.net.URL

data class PocketBaseSession(
    val baseUrl: String,
    val token: String,
    val userId: String,
    val email: String
)

class PocketBaseException(
    val statusCode: Int,
    message: String
) : IOException(message)

object PocketBaseAuth {
    const val DEFAULT_BASE_URL = "http://10.0.2.2:8090"
    private const val PREFS = "pocketbase-session"
    private const val KEY_BASE_URL = "base_url"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_EMAIL = "email"
    @Volatile private var applicationContext: Context? = null

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    fun baseUrl(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_BASE_URL, DEFAULT_BASE_URL)
            ?: DEFAULT_BASE_URL

    fun session(context: Context): PocketBaseSession? {
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val token = preferences.getString(KEY_TOKEN, null) ?: return null
        val userId = preferences.getString(KEY_USER_ID, null) ?: return null
        return PocketBaseSession(
            baseUrl = preferences.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
            token = token,
            userId = userId,
            email = preferences.getString(KEY_EMAIL, "").orEmpty()
        )
    }

    fun signOut(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_TOKEN)
            .remove(KEY_USER_ID)
            .remove(KEY_EMAIL)
            .apply()
    }

    suspend fun signIn(
        context: Context,
        baseUrl: String,
        email: String,
        password: String
    ): PocketBaseSession {
        val normalizedUrl = normalizeBaseUrl(baseUrl)
        val response = requestRaw(
            normalizedUrl,
            "/api/collections/users/auth-with-password",
            "POST",
            JSONObject().put("identity", email.trim()).put("password", password),
            null
        )
        return response.toSession(normalizedUrl).also { saveSession(context, it) }
    }

    suspend fun register(
        context: Context,
        baseUrl: String,
        email: String,
        password: String
    ): PocketBaseSession {
        val normalizedUrl = normalizeBaseUrl(baseUrl)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_BASE_URL, normalizedUrl)
            .apply()
        request(
            context,
            "/api/collections/users/records",
            "POST",
            JSONObject()
                .put("email", email.trim())
                .put("password", password)
                .put("passwordConfirm", password)
                .put("emailVisibility", true),
            authenticated = false
        )
        return signIn(context, normalizedUrl, email, password)
    }

    fun normalizeBaseUrl(value: String): String {
        val trimmed = value.trim().trimEnd('/')
        val parsed = try {
            URL(trimmed)
        } catch (_: MalformedURLException) {
            throw IllegalArgumentException("Địa chỉ PocketBase không hợp lệ.")
        }
        require(parsed.protocol == "http" || parsed.protocol == "https") {
            "Địa chỉ PocketBase phải bắt đầu bằng http:// hoặc https://."
        }
        require(parsed.host.isNotBlank()) { "Địa chỉ PocketBase không có tên máy chủ." }
        return trimmed
    }

    private fun saveSession(context: Context, session: PocketBaseSession) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_BASE_URL, session.baseUrl)
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_USER_ID, session.userId)
            .putString(KEY_EMAIL, session.email)
            .apply()
    }

    internal suspend fun request(
        context: Context,
        path: String,
        method: String = "GET",
        body: JSONObject? = null,
        authenticated: Boolean = true
    ): JSONObject {
        val activeSession = if (authenticated) session(context) else null
        check(!authenticated || activeSession != null) {
            "Hãy đăng nhập PocketBase trước."
        }
        if (activeSession == null) {
            return requestRaw(baseUrl(context), path, method, body, null)
        }
        try {
            return requestRaw(activeSession.baseUrl, path, method, body, activeSession.token)
        } catch (error: PocketBaseException) {
            if (error.statusCode != 401) throw error
        }
        val refreshed = requestRaw(
            activeSession.baseUrl,
            "/api/collections/users/auth-refresh",
            "POST",
            JSONObject(),
            activeSession.token
        ).toSession(activeSession.baseUrl)
        saveSession(context, refreshed)
        return requestRaw(refreshed.baseUrl, path, method, body, refreshed.token)
    }

    internal suspend fun request(
        path: String,
        method: String = "GET",
        body: JSONObject? = null
    ): JSONObject {
        val context = applicationContext
            ?: throw IllegalStateException("PocketBase chưa được khởi tạo.")
        return request(context, path, method, body)
    }

    private suspend fun requestRaw(
        baseUrl: String,
        path: String,
        method: String,
        body: JSONObject?,
        token: String?
    ): JSONObject = withContext(Dispatchers.IO) {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            setRequestProperty("Accept", "application/json")
            token?.let { setRequestProperty("Authorization", it) }
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
        }
        try {
            body?.let {
                connection.outputStream.use { output ->
                    output.write(it.toString().toByteArray(Charsets.UTF_8))
                }
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val responseText = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            val response = if (responseText.isBlank()) JSONObject() else {
                try {
                    JSONObject(responseText)
                } catch (_: JSONException) {
                    JSONObject()
                }
            }
            if (status !in 200..299) {
                val message = response.optString("message").ifBlank { "Lỗi HTTP $status." }
                throw PocketBaseException(status, "PocketBase: $message")
            }
            response
        } catch (error: PocketBaseException) {
            throw error
        } catch (error: IOException) {
            throw IOException("Không kết nối được PocketBase: ${error.localizedMessage ?: "lỗi mạng."}", error)
        } finally {
            connection.disconnect()
        }
    }

    private fun JSONObject.toSession(baseUrl: String): PocketBaseSession {
        val record = optJSONObject("record")
            ?: throw IOException("PocketBase không trả về thông tin tài khoản.")
        val token = optString("token")
        if (token.isBlank()) throw IOException("PocketBase không cấp token đăng nhập.")
        val id = record.optString("id")
        if (id.isBlank()) throw IOException("PocketBase trả về tài khoản không hợp lệ.")
        return PocketBaseSession(
            baseUrl = baseUrl,
            token = token,
            userId = id,
            email = record.optString("email")
        )
    }
}
