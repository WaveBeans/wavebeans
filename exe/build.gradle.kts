plugins {
    val kotlinVersion: String by System.getProperties()
    application

    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
}

application {
    mainClassName = "io.wavebeans.execution.distributed.FacilitatorCliKt"
    applicationName = "wavebeans-facilitator"
}

dependencies {

    implementation(project(":lib"))
    implementation(project(":proto"))

    val kotlinxSerializationRuntimeVersion: String by System.getProperties()

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationRuntimeVersion")

    // distributed execution dependencies
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationRuntimeVersion")

    implementation("commons-cli:commons-cli:1.4")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation("com.uchuhimo:konf-core:0.22.1")
    implementation("com.uchuhimo:konf-hocon:0.22.1")
}