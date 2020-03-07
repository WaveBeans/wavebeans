plugins {
    val kotlinVersion: String by System.getProperties()

    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
}

dependencies {

    implementation(project(":lib"))
    implementation(project(":exe"))

    val kotlinxSerializationRuntimeVersion: String by System.getProperties()
    val ktorVersion: String by System.getProperties()

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationRuntimeVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}