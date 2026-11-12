import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvmToolchain(21)
    android {
        namespace = "com.kjursa.foundation"
        compileSdk = 36
        minSdk = 26
        withHostTestBuilder {}.configure {}
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    val iosTargets = listOf(iosArm64(), iosSimulatorArm64())
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
            api(libs.kotlinx.coroutines.core)
            api(libs.compose.resources)
            api(libs.savedstate)
            api(libs.kotlinx.serialization.json)
            api(libs.lifecycle.viewmodel.savedstate)
            api(libs.ui)
            api(libs.compose.material3)
            api(libs.lifecycle.viewmodel)
            implementation(libs.lifecycle.runtime.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test-junit"))
        }
    }
}
