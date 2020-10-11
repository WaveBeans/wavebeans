tasks.jar {
    archiveFileName.set("metrics-core-${project.version}.jar")
}

dependencies {
    compileOnly(project(":proto")) {
        because("`lib` is dependent on this project but `proto` is required for " +
                "distributed execution only which will provide the `proto` implicitly")
    }

    testImplementation(project(":proto"))
}