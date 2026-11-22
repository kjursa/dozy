import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ktlint)
}

val apiBaseUrl = providers.gradleProperty("dozy.apiBaseUrl").orNull
val apiBaseUrlLiteral = apiBaseUrl?.let { "\"${it.replace("\\", "\\\\").replace("\"", "\\\"")}\"" } ?: "null"

val appLogging = providers.gradleProperty("dozy.logging").map { it.toBooleanStrict() }.orNull
val httpLogging = providers.gradleProperty("dozy.httpLogging").map { it.toBooleanStrict() }.orNull

android {
    namespace = "com.dozy.mobile"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dozy.mobile"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "DOZY_API_BASE_URL", apiBaseUrlLiteral)
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "DOZY_API_BASE_URL", "\"http://10.0.2.2:8080\"")
            buildConfigField("boolean", "ALLOW_LOCAL_ASSET_HTTP", "true")
            buildConfigField("boolean", "DOZY_LOGGING", (appLogging ?: true).toString())
            buildConfigField("boolean", "DOZY_HTTP_LOGGING", (httpLogging ?: true).toString())
        }
        getByName("release") {
            buildConfigField("boolean", "ALLOW_LOCAL_ASSET_HTTP", "false")
            buildConfigField("boolean", "DOZY_LOGGING", (appLogging ?: false).toString())
            buildConfigField("boolean", "DOZY_HTTP_LOGGING", (httpLogging ?: false).toString())
        }
    }

    signingConfigs {
        getByName("debug") {
            // Public test key shared by local and CI builds. Never use for production.
            storeFile = rootProject.file("signing/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.work.runtime.ktx)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose.viewmodel)
    implementation(libs.room.runtime)
    implementation(libs.sqlite.framework)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    testImplementation(libs.ktor.client.mock)
    implementation(project(":foundation"))
    implementation(project(":shared"))
    implementation(libs.activity.compose)
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    debugImplementation(libs.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.robolectric)
    testImplementation(libs.sqlite.framework)
    testImplementation(libs.kotlinx.coroutines.test)
}

kotlin {
    jvmToolchain(21)
    sourceSets.getByName("test").kotlin.srcDir("../shared/src/commonTest/kotlin/fixtures")
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
