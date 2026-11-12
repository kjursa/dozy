import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
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
