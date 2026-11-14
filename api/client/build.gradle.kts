import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins { kotlin("multiplatform") }
kotlin {
    jvmToolchain(21)
    jvm { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    iosArm64()
    iosSimulatorArm64()
    sourceSets {
        commonMain.dependencies {
            api(project(":contracts"))
            api("io.ktor:ktor-client-core:3.6.0")
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation("io.ktor:ktor-client-mock:3.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}
