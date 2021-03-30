plugins {
    val kotlinVersion: String by System.getProperties()

    kotlin("plugin.serialization") version kotlinVersion
}

dependencies {

    val kotlinxSerializationRuntimeVersion: String by System.getProperties()

    implementation(project(":filesystems:filesystems-core"))
    implementation(project(":metrics:metrics-core"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationRuntimeVersion")
}