import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion: String by System.getProperties()

    kotlin("jvm") version kotlinVersion
    id("com.jfrog.bintray") version "1.8.4"

    `java-library`
    `maven-publish`
}

allprojects {
    apply {
        plugin("kotlin")
    }

    repositories {
        maven ("https://dl.bintray.com/kotlin/kotlin-eap")
        maven ("https://kotlin.bintray.com/kotlinx")
        jcenter()
        mavenCentral()
    }

    val compileKotlin: KotlinCompile by tasks
    compileKotlin.kotlinOptions {
        jvmTarget = "1.8"
    }

    val compileTestKotlin: KotlinCompile by tasks
    compileTestKotlin.kotlinOptions {
        jvmTarget = "1.8"
    }

    tasks.withType<KotlinCompile>().all {
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalStdlibApi"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=io.ktor.util.KtorExperimentalAPI"
    }
}

subprojects {

    group = "io.wavebeans"

    val spekVersion: String by System.getProperties()

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("reflect"))
        implementation("io.github.microutils:kotlin-logging:1.7.7")

        testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
        testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")
        testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.13")
        testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")
        testImplementation("ch.qos.logback:logback-classic:1.2.3")

    }

    java {
        withJavadocJar()
        withSourcesJar()
    }

    tasks.test {
        systemProperty("SPEK_TIMEOUT", 0)
        useJUnitPlatform {
            includeEngines("spek2")
        }
        maxHeapSize = "2g"
    }

    tasks.jar {
        manifest {
            attributes(
                    "WaveBeans-Version" to properties["version"]
            )
        }
    }
}

publishing {
    publications {
        create<MavenPublication>("lib") {
            from(subprojects.first { it.name == "lib" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "lib"
        }
        create<MavenPublication>("exe") {
            from(subprojects.first { it.name == "exe" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "exe"
        }
        create<MavenPublication>("proto") {
            from(subprojects.first { it.name == "proto" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "proto"
        }
        create<MavenPublication>("http") {
            from(subprojects.first { it.name == "http" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "http"
        }
        create<MavenPublication>("filesystems.core") {
            from(subprojects.first { it.name == "core" && it.parent?.name == "filesystems" }.components["java"])
            groupId = "io.wavebeans.filesystems"
            artifactId = "core"
        }
        create<MavenPublication>("filesystems.dropbox") {
            from(subprojects.first { it.name == "dropbox" && it.parent?.name == "filesystems" }.components["java"])
            groupId = "io.wavebeans.filesystems"
            artifactId = "dropbox"
        }
        create<MavenPublication>("metrics.core") {
            from(subprojects.first { it.name == "core" && it.parent?.name == "metrics" }.components["java"])
            groupId = "io.wavebeans.metrics"
            artifactId = "core"
        }
        create<MavenPublication>("metrics.prometheus") {
            from(subprojects.first { it.name == "prometheus" && it.parent?.name == "metrics" }.components["java"])
            groupId = "io.wavebeans.metrics"
            artifactId = "prometheus"
        }
    }
}

bintray {
    user = findProperty("bintray.user")?.toString() ?: ""
    key = findProperty("bintray.key")?.toString() ?: ""
    setPublications("lib", "exe", "http", "proto", "filesystems.core", "filesystems.dropbox", "metrics.core", "metrics.prometheus")
    pkg(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.PackageConfig> {
        repo = "wavebeans"
        name = "wavebeans"
        userOrg = "wavebeans"
        vcsUrl = "https://github.com/WaveBeans/wavebeans"
        setLicenses("Apache-2.0")
        version(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.VersionConfig> {
            name = project.version.toString()
        })
    })
}
