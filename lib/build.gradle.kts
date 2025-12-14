plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.retry)
}

kotlin {
    jvm { }
    js(IR) { browser() }
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xlambdas=class")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
        // that attempts to fix flaky tests once and for all
        retry {
            maxRetries.set(3)
            maxFailures.set(10)
        }
    }

    sourceSets.all {
        languageSettings {
            optIn("kotlinx.serialization.ExperimentalSerializationApi")
        }
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
                implementation(libs.kotlin.logging)
                implementation(libs.kotlinx.serialization.json)
            }
        }
        commonTest {
        }
        jvmMain {
            dependencies {
                implementation(libs.kotlin.stdlib.jdk8)
                implementation(libs.kotlin.reflect)
            }
        }
        jvmTest {
            dependencies {
                implementation(project(":tests"))
                implementation(libs.kotest.runner.junit5)
                implementation(libs.logback.classic)
                implementation(libs.assertk)
                implementation(libs.mockito.kotlin)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        jsMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }
        jsTest {
            dependencies {
            }
        }
    }
}