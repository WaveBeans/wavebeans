dependencies {
    implementation(project(":lib"))
    implementation(project(":exe"))
    implementation(project(":proto"))
    implementation(project(":metrics-core"))
    implementation(project(":filesystems-core"))

    implementation("ch.qos.logback:logback-classic:1.2.3")

    implementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
}