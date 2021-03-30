tasks.jar {
    archiveBaseName.set("metrics-prometheus")
}

dependencies{
    implementation(project(":metrics:metrics-core"))

    api("io.prometheus:simpleclient:0.9.0")
    api("io.prometheus:simpleclient_hotspot:0.9.0")
    api("io.prometheus:simpleclient_httpserver:0.9.0")
}