tasks.jar {
    archiveBaseName.set("filesystems-dropbox")
}

dependencies {
    implementation(project(":lib"))
    implementation("com.dropbox.core:dropbox-core-sdk:3.1.4")
}