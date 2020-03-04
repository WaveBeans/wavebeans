plugins {
    application
}

application {
    mainClassName = "io.wavebeans.cli.CliKt"
    applicationName = "wavebeans"
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":exe"))

    implementation("commons-cli:commons-cli:1.4")
}
