plugins {
    val kotlinVersion: String by System.getProperties()

    kotlin("multiplatform")
    kotlin("plugin.serialization") version kotlinVersion
    id("org.gradle.test-retry") version "1.2.0"
}

kotlin {
    jvm {
    }
    js(IR) { browser() }

    tasks.withType<Test> {
        systemProperty("SPEK_TIMEOUT", 0)
        useJUnitPlatform {
            includeEngines("spek2")
        }
        maxHeapSize = "2g"
        // that attempts to fix flaky tests once and for all
        retry {
            maxRetries.set(3)
            maxFailures.set(10)
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                val kotlinxSerializationRuntimeVersion: String by System.getProperties()
                implementation(kotlin("stdlib"))
                implementation("io.github.microutils:kotlin-logging:3.0.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationRuntimeVersion")
            }
        }
        val commonTest by getting {
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation(kotlin("reflect"))
            }
        }
        val jvmTest by getting {
            dependencies {
                val spekVersion: String by System.getProperties()
                implementation(project(":tests"))
                implementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
                runtimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")
                implementation("com.willowtreeapps.assertk:assertk-jvm:0.13")
                implementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
                implementation("ch.qos.logback:logback-classic:1.2.3")
            }
        }
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
            }
        }
        val jsTest by getting {
        }
    }
}
//dependencies {
//
//
//    implementation(project(":filesystems-core"))
//    implementation(project(":metrics-core"))
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationRuntimeVersion")
//}