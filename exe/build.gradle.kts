plugins {
    application
    alias(libs.plugins.kotlin.serialization)
}

application {
    mainClass.set("io.wavebeans.execution.distributed.FacilitatorCliKt")
    applicationName = "wavebeans-facilitator"
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":proto"))
    implementation(project(":metrics-core"))

    implementation(libs.kotlinx.serialization.json)

    // distributed execution dependencies
    implementation(libs.kotlinx.serialization.protobuf)

    implementation(libs.commons.cli)
    implementation(libs.logback.classic)

    implementation(libs.bundles.konf)
}