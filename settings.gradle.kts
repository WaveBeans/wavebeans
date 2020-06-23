include(":lib", ":cli", ":exe", ":http", ":distr", ":proto")
include(":filesystems")
include(":filesystems:core")
include(":filesystems:dropbox")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven ("https://dl.bintray.com/kotlin/kotlin-eap")
    }
}