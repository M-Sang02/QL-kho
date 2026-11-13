package com.kho.glass.ui

import android.content.Context
import android.net.Uri
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import java.io.File

@Composable
fun KhoRoot(
    sharedImageUri: Uri? = null,
    onSharedImageHandled: () -> Unit = {},
    sharedText: String? = null,
    onSharedTextHandled: () -> Unit = {}
) {
    val context = LocalContext.current
    val firebaseStatus = remember(context) {
        try {
            (FirebaseApp.getApps(context).isNotEmpty() ||
                FirebaseApp.initializeApp(context) != null) to null
        } catch (error: FirebaseException) {
            false to (error.localizedMessage ?: "Kiểm tra Firebase configuration.")
        } catch (error: IllegalStateException) {
            false to (error.localizedMessage ?: "Kiểm tra Firebase configuration.")
        }
    }
    val firebaseConfigured = firebaseStatus.first
    var showFirebaseError by remember(firebaseStatus.second) {
        mutableStateOf(firebaseStatus.second != null)
    }
    val preferences = remember(context) {
        context.getSharedPreferences("kho-glass-preferences", Context.MODE_PRIVATE)
    }
    val offlineStorageId = if (preferences.getBoolean("offline_storage_transferred", false)) {
        "offline-device"
    } else {
        "local-device"
    }

    if (!firebaseConfigured) {
        KhoApp(
            userId = offlineStorageId,
            localOnly = true,
            sharedImageUri = sharedImageUri,
            onSharedImageHandled = onSharedImageHandled,
            sharedText = sharedText,
            onSharedTextHandled = onSharedTextHandled
        )
        if (showFirebaseError) {
            AlertDialog(
                onDismissRequest = { showFirebaseError = false },
                title = { Text("Không khởi tạo được Firebase") },
                text = { Text(firebaseStatus.second.orEmpty()) },
                confirmButton = {
                    TextButton(onClick = { showFirebaseError = false }) { Text("Đóng") }
                }
            )
        }
        return
    }

    val auth = remember { FirebaseAuth.getInstance() }
    val scope = rememberCoroutineScope()
    var user by remember(auth) { mutableStateOf(auth.currentUser) }
    var continueOffline by remember { mutableStateOf(true) }
    var connectingCloudSync by remember { mutableStateOf(false) }
    var cloudSyncError by remember { mutableStateOf<String?>(null) }
    var storageUserId by remember { mutableStateOf("local-device") }
    var resolvingStorage by remember(user?.uid) { mutableStateOf(user != null) }
    var pendingStorageChoice by remember { mutableStateOf<Pair<FirebaseUser, String>?>(null) }
    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { user = it.currentUser }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val currentUser = user
    LaunchedEffect(currentUser?.uid) {
        val signedInUser = currentUser ?: return@LaunchedEffect
        resolvingStorage = true
        val mappedStorageId = preferences.getString("account_storage_${signedInUser.uid}", null)
        if (!mappedStorageId.isNullOrBlank()) {
            storageUserId = mappedStorageId
            preferences.edit().remove("account_adoption_source").apply()
        } else if (preferences.getBoolean("account_storage_transferred_${signedInUser.uid}", false)) {
            storageUserId = "account-${signedInUser.uid}"
            preferences.edit()
                .putString("account_storage_${signedInUser.uid}", storageUserId)
                .remove("account_adoption_source")
                .apply()
        } else if (hasLocalWarehouse(context, signedInUser.uid)) {
            storageUserId = signedInUser.uid
            preferences.edit()
                .putString("account_storage_${signedInUser.uid}", signedInUser.uid)
                .remove("account_adoption_source")
                .apply()
        } else {
            val sourceId = preferences.getString("account_adoption_source", "local-device")
                ?: "local-device"
            if (sourceId != signedInUser.uid && hasLocalWarehouse(context, sourceId)) {
                pendingStorageChoice = signedInUser to sourceId
            } else {
                storageUserId = signedInUser.uid
                preferences.edit()
                    .putString("account_storage_${signedInUser.uid}", signedInUser.uid)
                    .apply()
            }
        }
        resolvingStorage = false
    }

    pendingStorageChoice?.let { (signedInUser, sourceId) ->
        val accountLabel = signedInUser.email
            ?: if (signedInUser.isAnonymous) "thiết bị này" else "này"
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Chọn kho cho tài khoản") },
            text = {
                Text(
                    "Tài khoản $accountLabel chưa có dữ liệu kho cục bộ trên thiết bị. " +
                        "Bạn muốn dùng kho hiện tại trên máy hay bắt đầu một kho cục bộ mới?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    assignWarehouseToAccount(preferences, signedInUser.uid, sourceId)
                    preferences.edit().remove("account_adoption_source").apply()
                    storageUserId = sourceId
                    pendingStorageChoice = null
                }) { Text("Dùng kho hiện tại") }
            },
            dismissButton = {
                TextButton(onClick = {
                    assignWarehouseToAccount(preferences, signedInUser.uid, signedInUser.uid)
                    preferences.edit().remove("account_adoption_source").apply()
                    storageUserId = signedInUser.uid
                    pendingStorageChoice = null
                }) { Text("Tạo kho mới") }
            }
        )
    }

    if (currentUser == null && !continueOffline) {
        GoogleSignInScreen(
            auth = auth,
            onContinueOffline = { continueOffline = true }
        )
    } else if (resolvingStorage || pendingStorageChoice != null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        KhoApp(
            userId = if (currentUser == null) offlineStorageId else storageUserId,
            localOnly = currentUser == null,
            accountDisplayName = currentUser?.let {
                if (it.isAnonymous) "Đồng bộ qua mã" else it.displayName ?: it.email
            },
            accountEmail = currentUser?.email,
            accountKey = currentUser?.uid ?: "local-device",
            accountPhotoUrl = currentUser?.photoUrl?.toString(),
            onRequestGoogleSignIn = if (currentUser == null || currentUser.isAnonymous) null else ({
                preferences.edit()
                    .putString(
                        "account_adoption_source",
                        if (currentUser == null) offlineStorageId else storageUserId
                    )
                    .apply()
                auth.signOut()
                continueOffline = false
            }),
            onEnableCodeSync = {
                cloudSyncError = null
                preferences.edit()
                    .putString("account_adoption_source", offlineStorageId)
                    .apply()
                connectingCloudSync = true
                scope.launch {
                    try {
                        auth.signInAnonymously().await()
                        continueOffline = false
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: FirebaseAuthException) {
                        cloudSyncError = if (error.errorCode == "ERROR_OPERATION_NOT_ALLOWED") {
                            "Hãy bật đăng nhập Ẩn danh trong Firebase Console → Authentication → Sign-in method."
                        } else {
                            "Không bật được đồng bộ: ${error.localizedMessage ?: error.errorCode}"
                        }
                    } catch (error: FirebaseException) {
                        cloudSyncError =
                            "Không kết nối được Firebase: ${error.localizedMessage ?: "hãy kiểm tra Internet."}"
                    } catch (error: IllegalStateException) {
                        cloudSyncError =
                            "Không bật được đồng bộ: ${error.localizedMessage ?: "lỗi cấu hình Firebase."}"
                    } finally {
                        connectingCloudSync = false
                    }
                }
            },
            onSignOut = if (currentUser?.isAnonymous == true) null else ({
                preferences.edit()
                    .putString("account_adoption_source", storageUserId)
                    .apply()
                auth.signOut()
                continueOffline = true
            }),
            sharedImageUri = sharedImageUri,
            onSharedImageHandled = onSharedImageHandled,
            sharedText = sharedText,
            onSharedTextHandled = onSharedTextHandled
        )
    }

    if (connectingCloudSync) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Đang bật đồng bộ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Đang kết nối tài khoản thiết bị với Firebase.")
                }
            },
            confirmButton = {}
        )
    }
    cloudSyncError?.let { message ->
        AlertDialog(
            onDismissRequest = { cloudSyncError = null },
            title = { Text("Chưa bật được đồng bộ") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { cloudSyncError = null }) { Text("Đóng") }
            }
        )
    }
}

@Composable
private fun GoogleSignInScreen(
    auth: FirebaseAuth,
    onContinueOffline: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val webClientId = remember(context) {
        val resourceId = context.resources.getIdentifier(
            "default_web_client_id",
            "string",
            context.packageName
        )
        resourceId.takeIf { it != 0 }?.let { context.getString(it) }
    }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var signingIn by remember { mutableStateOf(false) }

    val googleIdOption = remember(webClientId) {
        webClientId?.takeIf { it.isNotBlank() }?.let { clientId ->
            GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(clientId)
                .setAutoSelectEnabled(false)
                .build()
        }
    }

    if (googleIdOption == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Cần cấu hình Firebase", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(12.dp))
            Text("Firebase chưa tạo default_web_client_id. Đăng nhập Google chưa khả dụng.")
            Spacer(Modifier.height(20.dp))
            Button(onClick = onContinueOffline, modifier = Modifier.fillMaxWidth()) {
                Text("Tiếp tục ngoại tuyến")
            }
        }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Quản lý kho", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        Text("Đăng nhập bằng tài khoản Google được cấp quyền vào kho.")
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = {
                errorMessage = null
                signingIn = true
                scope.launch {
                    try {
                        val credentialManager = CredentialManager.create(context)
                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()
                        val result = credentialManager.getCredential(context, request)
                        val credential = result.credential
                        if (
                            credential !is CustomCredential ||
                            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                        ) {
                            errorMessage = "Google không trả về thông tin đăng nhập hợp lệ."
                        } else {
                            val googleCredential =
                                GoogleIdTokenCredential.createFrom(credential.data)
                            val firebaseCredential = GoogleAuthProvider.getCredential(
                                googleCredential.idToken,
                                null
                            )
                            auth.signInWithCredential(firebaseCredential).await()
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: NoCredentialException) {
                        errorMessage =
                            "Không tìm thấy tài khoản Google trên thiết bị. Hãy thêm tài khoản Google hoặc tiếp tục ngoại tuyến."
                    } catch (error: GetCredentialException) {
                        errorMessage = "Không thể mở bộ chọn tài khoản Google: " +
                            (error.localizedMessage ?: error.type)
                    } catch (error: GoogleIdTokenParsingException) {
                        errorMessage = "Không đọc được thông tin đăng nhập Google: " +
                            (error.localizedMessage ?: "dữ liệu không hợp lệ")
                    } catch (error: FirebaseException) {
                        errorMessage = "Firebase không xác thực được tài khoản: " +
                            (error.localizedMessage ?: "lỗi xác thực")
                    } catch (error: IllegalStateException) {
                        errorMessage = "Dịch vụ đăng nhập Google không khả dụng: " +
                            (error.localizedMessage ?: "hãy kiểm tra Google Play Services và cấu hình Firebase")
                    } finally {
                        signingIn = false
                    }
                }
            },
            enabled = !signingIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Đăng nhập Google")
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onContinueOffline,
            enabled = !signingIn,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tiếp tục ngoại tuyến")
        }
        Text(
            "Dữ liệu ngoại tuyến chỉ lưu trên thiết bị này. Đăng nhập Google cần kết nối mạng lần đầu.",
            style = MaterialTheme.typography.bodySmall
        )
        if (signingIn) {
            Spacer(Modifier.height(16.dp))
            CircularProgressIndicator()
        }
        errorMessage?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun hasLocalWarehouse(context: android.content.Context, userId: String): Boolean {
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
        if (key.startsWith("account_storage_") &&
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

