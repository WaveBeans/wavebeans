plugins {
    application
}

application {
    mainClassName = "io.wavebeans.cli.CliKt"
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":exe"))

    implementation(group = "org.jline", name = "jline", version = "3.10.0")
}
