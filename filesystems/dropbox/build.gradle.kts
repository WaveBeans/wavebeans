tasks.jar {
    archiveBaseName.set("filesystems-dropbox")
}

dependencies {
    implementation(project(":filesystems-core"))
    implementation("com.dropbox.core:dropbox-core-sdk:3.1.4")
}