package com.kho.glass.ui

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.kho.glass.data.PocketBaseAuth
import com.kho.glass.data.PocketBaseSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.UUID

@Composable
fun KhoRoot(
    sharedImageUri: Uri? = null,
    onSharedImageHandled: () -> Unit = {},
    sharedText: String? = null,
    onSharedTextHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    PocketBaseAuth.initialize(context)
    val preferences = remember(context) {
        context.getSharedPreferences("kho-glass-preferences", Context.MODE_PRIVATE)
    }
    val offlineStorageId = if (preferences.getBoolean("offline_storage_transferred", false)) {
        "offline-device"
    } else {
        "local-device"
    }
    var session by remember(context) { mutableStateOf(PocketBaseAuth.session(context)) }
    var showLogin by remember { mutableStateOf(false) }
    val currentSession = session
    var storageUserId by remember(session?.userId) {
        mutableStateOf(session?.let { storedSession ->
            preferences.getString("account_storage_${storedSession.userId}", storedSession.userId)
                ?: storedSession.userId
        } ?: offlineStorageId)
    }
    var pendingStorageChoice by remember { mutableStateOf<Pair<PocketBaseSession, String>?>(null) }

    LaunchedEffect(currentSession?.userId, offlineStorageId) {
        if (currentSession != null && pendingStorageChoice == null) {
            val savedStorageId =
                preferences.getString("account_storage_${currentSession.userId}", null)
            if (savedStorageId.isNullOrBlank() && storageUserId == currentSession.userId &&
                hasLocalWarehouse(context, offlineStorageId)
            ) {
                pendingStorageChoice = currentSession to offlineStorageId
            }
        }
    }

    pendingStorageChoice?.let { (signedInSession, sourceId) ->
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Chọn kho cho tài khoản") },
            text = {
                Text(
                    "Tài khoản ${signedInSession.email} chưa có kho trên thiết bị. " +
                        "Bạn muốn dùng kho hiện có hay tạo kho cục bộ mới?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    assignWarehouseToAccount(preferences, signedInSession.userId, sourceId)
                    storageUserId = sourceId
                    pendingStorageChoice = null
                }) { Text("Dùng kho hiện tại") }
            },
            dismissButton = {
                TextButton(onClick = {
                    assignWarehouseToAccount(
                        preferences,
                        signedInSession.userId,
                        signedInSession.userId
                    )
                    storageUserId = signedInSession.userId
                    pendingStorageChoice = null
                }) { Text("Tạo kho mới") }
            }
        )
    }

    if (showLogin) {
        PocketBaseSignInScreen(
            initialBaseUrl = PocketBaseAuth.baseUrl(context),
            onContinueOffline = { showLogin = false },
            onAuthenticated = { authenticatedSession ->
                session = authenticatedSession
                val mapped = preferences.getString(
                    "account_storage_${authenticatedSession.userId}",
                    null
                )
                val source = preferences.getString("account_adoption_source", offlineStorageId)
                    ?: offlineStorageId
                when {
                    !mapped.isNullOrBlank() -> storageUserId = mapped
                    hasLocalWarehouse(context, authenticatedSession.userId) ->
                        storageUserId = authenticatedSession.userId
                    hasLocalWarehouse(context, source) ->
                        pendingStorageChoice = authenticatedSession to source
                    else -> {
                        storageUserId = authenticatedSession.userId
                        assignWarehouseToAccount(
                            preferences,
                            authenticatedSession.userId,
                            authenticatedSession.userId
                        )
                    }
                }
                preferences.edit().remove("account_adoption_source").apply()
                showLogin = false
            }
        )
    } else if (pendingStorageChoice != null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        KhoApp(
            userId = if (currentSession == null) offlineStorageId else storageUserId,
            localOnly = currentSession == null,
            accountDisplayName = currentSession?.email,
            accountEmail = currentSession?.email,
            accountKey = currentSession?.userId ?: "local-device",
            onRequestSignIn = { showLogin = true },
            onEnableCodeSync = { showLogin = true },
            onSignOut = {
                if (currentSession != null) {
                    preferences.edit()
                        .putString("account_adoption_source", storageUserId)
                        .apply()
                }
                PocketBaseAuth.signOut(context)
                session = null
                storageUserId = offlineStorageId
            },
            sharedImageUri = sharedImageUri,
            onSharedImageHandled = onSharedImageHandled,
            sharedText = sharedText,
            onSharedTextHandled = onSharedTextHandled
        )
    }
}

@Composable
private fun PocketBaseSignInScreen(
    initialBaseUrl: String,
    onContinueOffline: () -> Unit,
    onAuthenticated: (PocketBaseSession) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var baseUrl by remember(initialBaseUrl) { mutableStateOf(initialBaseUrl) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var connecting by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Đăng nhập PocketBase", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Dùng cùng tài khoản PocketBase trên các thiết bị để đồng bộ dữ liệu kho."
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = baseUrl,
            onValueChange = { baseUrl = it },
            label = { Text("Địa chỉ PocketBase") },
            supportingText = { Text("Máy ảo Android: http://10.0.2.2:8090") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Mật khẩu") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                errorMessage = null
                connecting = true
                scope.launch {
                    try {
                        val signedIn = PocketBaseAuth.signIn(context, baseUrl, email, password)
                        onAuthenticated(signedIn)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: IOException) {
                        errorMessage = error.localizedMessage ?: "Không đăng nhập được."
                    } catch (error: IllegalArgumentException) {
                        errorMessage = error.message ?: "Địa chỉ PocketBase không hợp lệ."
                    } catch (error: IllegalStateException) {
                        errorMessage = error.localizedMessage ?: "Không đăng nhập được."
                    } finally {
                        connecting = false
                    }
                }
            },
            enabled = !connecting && email.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (connecting) "Đang đăng nhập..." else "Đăng nhập")
        }
        TextButton(
            onClick = {
                errorMessage = null
                connecting = true
                scope.launch {
                    try {
                        val created = PocketBaseAuth.register(context, baseUrl, email, password)
                        onAuthenticated(created)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: IOException) {
                        errorMessage = error.localizedMessage ?: "Không tạo được tài khoản."
                    } catch (error: IllegalArgumentException) {
                        errorMessage = error.message ?: "Dữ liệu tài khoản không hợp lệ."
                    } catch (error: IllegalStateException) {
                        errorMessage = error.localizedMessage ?: "Không tạo được tài khoản."
                    } finally {
                        connecting = false
                    }
                }
            },
            enabled = !connecting && email.isNotBlank() && password.length >= 8,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tạo tài khoản")
        }
        TextButton(
            onClick = onContinueOffline,
            enabled = !connecting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tiếp tục ngoại tuyến")
        }
        Text(
            "Không dùng 127.0.0.1 trên điện thoại khác: địa chỉ đó trỏ về chính thiết bị. " +
                "Hãy nhập IP LAN hoặc tên miền HTTPS của máy chủ PocketBase.",
            style = MaterialTheme.typography.bodySmall
        )
        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

private fun hasLocalWarehouse(context: Context, userId: String): Boolean {
    val userKey = UUID.nameUUIDFromBytes(userId.toByteArray())
    return File(context.getDatabasePath("kho-glass-$userKey.db").path).exists()
}

private fun assignWarehouseToAccount(
    preferences: android.content.SharedPreferences,
    accountId: String,
    storageId: String
) {
    val editor = preferences.edit()
    preferences.all.forEach { (key, value) ->
        if (
            key.startsWith("account_storage_") &&
            key != "account_storage_$accountId" &&
            value == storageId
        ) {
            editor.remove(key)
            editor.putBoolean(
                "account_storage_transferred_${key.removePrefix("account_storage_")}",
                true
            )
        }
    }
    if (storageId == "local-device") {
        editor.putBoolean("offline_storage_transferred", true)
    }
    editor.putString("account_storage_$accountId", storageId).apply()
}
