plugins {
    val kotlinVersion: String by System.getProperties()

    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
}

dependencies {

    implementation(project(":lib"))

    val kotlinxSerializationRuntimeVersion: String by System.getProperties()

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationRuntimeVersion")

    implementation("io.github.microutils:kotlin-logging:1.7.7")

    testImplementation("ch.qos.logback:logback-classic:1.2.3")

}