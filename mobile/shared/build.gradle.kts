import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

kotlin {
    jvmToolchain(21)
    android {
        namespace = "com.dozy.shared"
        compileSdk = 36
        minSdk = 26
        withHostTestBuilder {}.configure {}
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    val iosTargets = listOf(iosArm64(), iosSimulatorArm64())
    iosTargets.forEach { target ->
        target.binaries.framework {
            baseName = "DozyShared"
            isStatic = true
        }
    }
    iosTargets.forEach { target ->
        target.binaries.configureEach {
            freeCompilerArgs +=
                "-Xoverride-konan-properties=minVersion.ios=15.0;osVersionMin.ios_arm64=15.0;osVersionMin.ios_simulator_arm64=15.0"
        }
        target.compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions.freeCompilerArgs.add(
                    "-Xoverride-konan-properties=minVersion.ios=15.0;osVersionMin.ios_arm64=15.0;osVersionMin.ios_simulator_arm64=15.0",
                )
            }
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":foundation"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            api(libs.room.runtime)
            api(libs.sqlite.bundled)
            api(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            api(libs.koin.core)
        }
        androidMain.dependencies { implementation(libs.ktor.client.android) }
        iosMain.dependencies { implementation(libs.ktor.client.darwin) }
        commonTest.dependencies {
            implementation(libs.ktor.client.mock)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test-junit"))
        }
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

// KSP 2.3.12 does not attach Android KMP output to AGP 8.13's compileAndroidMain.
// Wire the generated constructors/DAOs explicitly so clean builds also process Room.
kotlin.sourceSets
    .getByName("androidMain")
    .kotlin
    .srcDir(layout.buildDirectory.dir("generated/ksp/android/androidMain/kotlin"))
tasks.named("compileAndroidMain") { dependsOn("kspAndroidMain") }
