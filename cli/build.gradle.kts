plugins {
    application
}

application {
    mainClassName = "io.wavebeans.cli.CliKt"
    applicationName = "wavebeans"
}

repositories {
    maven {
        setUrl("https://dl.bintray.com/s1m0nw1/KtsRunner")
    }
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":exe"))

    implementation("de.swirtz:ktsRunner:0.0.7")
    implementation("commons-cli:commons-cli:1.4")
}
