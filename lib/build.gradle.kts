plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":filesystems-core"))
    implementation(project(":metrics-core"))
    implementation(libs.kotlinx.serialization.json)
}