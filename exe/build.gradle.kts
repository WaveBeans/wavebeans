plugins {
    application
    alias(libs.plugins.kotlin.serialization)
}

application {
    mainClass.set("io.wavebeans.execution.distributed.FacilitatorCliKt")
    applicationName = "wavebeans-facilitator"
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xlambdas=class")
    }
    sourceSets.all {
        languageSettings {
            optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }
}

dependencies {
    implementation(project(":lib"))
    implementation(project(":proto"))
    implementation(project(":metrics-core"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.protobuf)
    implementation(libs.kotlin.reflect)

    implementation(libs.commons.cli)
    implementation(libs.logback.classic)

    implementation(libs.bundles.konf)

    testImplementation(project(":filesystems-core"))

    // https://mvnrepository.com/artifact/org.ow2.asm/asm
    implementation("org.ow2.asm:asm:9.9.1")
}