plugins {
    val kotlinVersion: String by System.getProperties()

    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
}

dependencies {

    val kotlinxSerializationRuntimeVersion: String by System.getProperties()

    implementation(project(":filesystems:core"))
    implementation(project(":metrics:core"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationRuntimeVersion")
}