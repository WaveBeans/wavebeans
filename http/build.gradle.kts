plugins {
    val kotlinVersion: String by System.getProperties()

    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
}

dependencies {

    implementation(project(":lib"))
    implementation(project(":exe"))
    implementation(project(":proto"))
    implementation(project(":metrics-core"))

    val kotlinxSerializationRuntimeVersion: String by System.getProperties()

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationRuntimeVersion")

    implementation("io.netty:netty-all:4.1.82.Final")
    implementation("io.javalin:javalin:3.13.7")

    testImplementation("org.http4k:http4k-core:4.0.0.0")
    testImplementation("org.http4k:http4k-client-okhttp:4.0.0.0")
}