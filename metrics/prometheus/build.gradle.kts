tasks.jar {
    archiveFileName.set("metrics-prometheus-${project.version}.jar")
}

dependencies{
    implementation(project(":metrics:core"))

    api("io.prometheus:simpleclient:0.9.0")
    api("io.prometheus:simpleclient_hotspot:0.9.0")
    api("io.prometheus:simpleclient_httpserver:0.9.0")
}