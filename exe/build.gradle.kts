plugins {
    val kotlinVersion: String by System.getProperties()

    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
}

dependencies {

    val ktorVersion: String by System.getProperties()

    implementation(project(":lib"))

    val kotlinxSerializationRuntimeVersion: String by System.getProperties()

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:$kotlinxSerializationRuntimeVersion")

    // distributed execution dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationRuntimeVersion")

    implementation("commons-cli:commons-cli:1.4")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation("com.uchuhimo:konf:0.22.1")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")

    implementation("com.squareup.retrofit2:retrofit:2.8.1")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.5.0")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

}