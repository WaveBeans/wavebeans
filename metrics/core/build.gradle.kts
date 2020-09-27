tasks.jar {
    archiveFileName.set("metrics-core-${project.version}.jar")
}

dependencies {
    implementation(project(":proto"))
}