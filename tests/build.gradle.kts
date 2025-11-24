dependencies {
    implementation(project(":lib"))
    implementation(project(":exe"))
    implementation(project(":proto"))
    implementation(project(":metrics-core"))
    implementation(project(":filesystems-core"))

    implementation(libs.logback.classic)

    implementation(libs.assertk)
}