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
    implementation("commons-cli:commons-cli:1.4")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation("com.uchuhimo:konf:0.22.1")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization:$ktorVersion")

    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")

}