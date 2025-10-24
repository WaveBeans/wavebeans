tasks.jar {
    archiveBaseName.set("filesystems-dropbox")
}

dependencies {
    implementation(project(":filesystems-core"))
    implementation(libs.dropbox.core.sdk)
}