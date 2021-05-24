tasks.jar {
    archiveFileName.set("filesystems-dropbox-${project.version}.jar")
}

dependencies {
    implementation(project(":filesystems-core"))
    implementation("com.dropbox.core:dropbox-core-sdk:3.1.4")
}