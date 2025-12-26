plugins {
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.retry)
    alias(libs.plugins.atomicfu)
}

kotlin {
    jvm { }
    js(IR) {
        nodejs {
            testTask {
                useMocha()
            }
        }
        browser {
            testTask {
                useKarma {
                    useChromeHeadless()
                }
            }
        }
    }
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        freeCompilerArgs.add("-Xlambdas=class")
    }

    tasks.withType<AbstractTestTask> {
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }
    tasks.withType<Test> {
        useJUnitPlatform()
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
                implementation(libs.kotlinx.atomicfu)
            }
        }
        commonTest {
        }
        jsMain {
            dependencies {
                implementation(libs.kotlin.stdlib)
            }
        }
        jsTest {
            dependencies {
                // Use kotlin.test for JS
                implementation(libs.kotlin.test.js)
            }
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
    }
}