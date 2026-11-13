plugins {
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.serialization") version "2.3.21"
    application
}

repositories {
    mavenCentral()
}

val ktorVersion = "3.6.0"
val exposedVersion = "1.6.1"

dependencies {
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-config-yaml:$ktorVersion")
    implementation("io.ktor:ktor-server-di:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.flywaydb:flyway-core:13.6.0")
    implementation("org.xerial:sqlite-jdbc:3.54.1.0")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    testImplementation(kotlin("test-junit5"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.4")
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.dozy.ApplicationKt")
}

tasks.test {
    useJUnitPlatform()
}
