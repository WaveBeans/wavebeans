plugins {
    application
}

application {
    mainClass.set("io.wavebeans.cli.CliKt")
    applicationName = "wavebeans"
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":exe"))
    implementation(project(":http"))
    implementation(project(":filesystems-core"))
    implementation(project(":filesystems-dropbox"))
    implementation(project(":metrics-core"))

    implementation(libs.commons.cli)
    implementation(libs.logback.classic)

    implementation(libs.kotlin.scripting.jvm)
    implementation(libs.kotlin.scripting.jvm.host)
    implementation(libs.kotlin.scripting.common)
    implementation(libs.kotlin.scripting.compiler.embeddable)
    implementation(libs.kotlin.compiler.embeddable)
    implementation(libs.kotlin.main.kts)

    testImplementation(libs.bundles.http4k)
}
