plugins {
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":exe"))
    implementation(project(":proto"))
    implementation(project(":metrics-core"))

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.netty.all)
    implementation(libs.javalin)

    testImplementation(libs.bundles.http4k)
}