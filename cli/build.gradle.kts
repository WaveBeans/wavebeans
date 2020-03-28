plugins {
    application
}

application {
    mainClassName = "io.wavebeans.cli.CliKt"
    applicationName = "wavebeans"
}

dependencies {
    val ktorVersion: String by System.getProperties()

    implementation(project(":lib"))
    implementation(project(":exe"))
    implementation(project(":http"))

    implementation("commons-cli:commons-cli:1.4")
    implementation("ch.qos.logback:logback-classic:1.2.3")

    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
}
