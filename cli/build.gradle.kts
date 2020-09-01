plugins {
    application
}

application {
    mainClassName = "io.wavebeans.cli.CliKt"
    applicationName = "wavebeans"
}

dependencies {
    val ktorVersion: String by System.getProperties()
    val kotlinVersion: String by System.getProperties()

    implementation(project(":lib"))
    implementation(project(":exe"))
    implementation(project(":http"))
    implementation(project(":filesystems:core"))
    implementation(project(":filesystems:dropbox"))

    implementation("commons-cli:commons-cli:1.4")
    implementation("ch.qos.logback:logback-classic:1.2.3")

//    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host-embeddable:$kotlinVersion")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host-embeddable:$kotlinVersion")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-common:$kotlinVersion")
//    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$kotlinVersion")
//    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
//    implementation("org.jetbrains.kotlin:kotlin-main-kts:$kotlinVersion")
    // at the time 1.4.0 was not pulibhsed correctly, but using previous version of kotlin here is correct.
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host-embeddable:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm-host-embeddable:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-scripting-common:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.3.72")
    implementation("org.jetbrains.kotlin:kotlin-main-kts:1.3.72")

    testImplementation("io.ktor:ktor-client-cio:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
}
