import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val signingProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.isFile) {
        propertiesFile.inputStream().use { load(it) }
    }
}

fun signingValue(propertyName: String, environmentName: String): String? =
    System.getenv(environmentName)?.takeIf { it.isNotBlank() }
        ?: signingProperties.getProperty(propertyName)?.trim()?.takeIf { it.isNotBlank() }

data class SigningValues(
    val storeFile: String,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String
)

fun signingValues(variant: String): SigningValues? {
    val prefix = "QL_KHO_${variant.uppercase()}"
    val values = listOf(
        signingValue("${prefix}_STORE_FILE", "${prefix}_STORE_FILE"),
        signingValue("${prefix}_STORE_PASSWORD", "${prefix}_STORE_PASSWORD"),
        signingValue("${prefix}_KEY_ALIAS", "${prefix}_KEY_ALIAS"),
        signingValue("${prefix}_KEY_PASSWORD", "${prefix}_KEY_PASSWORD")
    )
    if (values.all { it == null }) return null
    require(values.all { it != null }) {
        "Signing configuration $prefix is incomplete. Set all four properties in local.properties or environment variables."
    }
    return SigningValues(
        storeFile = values[0]!!,
        storePassword = values[1]!!,
        keyAlias = values[2]!!,
        keyPassword = values[3]!!
    )
}

val debugSigningValues = signingValues("debug")
val releaseSigningValues = signingValues("release")
val releaseTaskRequested = gradle.startParameter.taskNames.any {
    it.contains("release", ignoreCase = true)
}
if (releaseTaskRequested && releaseSigningValues == null) {
    throw GradleException(
        "Release signing is not configured. Set QL_KHO_RELEASE_* values in local.properties or environment variables."
    )
}

android {
    namespace = "com.kho.glass"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kho.glass"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        if (debugSigningValues != null) {
            create("fixedDebug") {
                storeFile = rootProject.file(debugSigningValues.storeFile)
                storeType = "PKCS12"
                storePassword = debugSigningValues.storePassword
                keyAlias = debugSigningValues.keyAlias
                keyPassword = debugSigningValues.keyPassword
            }
        }
        if (releaseSigningValues != null) {
            create("fixedRelease") {
                storeFile = rootProject.file(releaseSigningValues.storeFile)
                storeType = "PKCS12"
                storePassword = releaseSigningValues.storePassword
                keyAlias = releaseSigningValues.keyAlias
                keyPassword = releaseSigningValues.keyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            if (debugSigningValues != null) {
                signingConfig = signingConfigs.getByName("fixedDebug")
            }
        }
        getByName("release") {
            if (releaseSigningValues != null) {
                signingConfig = signingConfigs.getByName("fixedRelease")
            }
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.01.00"))
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.vanniktech:android-image-cropper:4.7.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.1")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
