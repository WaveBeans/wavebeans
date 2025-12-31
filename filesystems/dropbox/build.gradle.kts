tasks.jar {
    archiveBaseName.set("filesystems-dropbox")
}

dependencies {
    implementation(project(":filesystems-core"))
    implementation(project(":lib"))
    implementation(libs.dropbox.core.sdk)
}