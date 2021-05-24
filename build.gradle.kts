import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion: String by System.getProperties()

    kotlin("jvm") version kotlinVersion
    id("org.gradle.test-retry") version "1.2.0"

    `java-library`
    `maven-publish`
}

allprojects {
    apply {
        plugin("kotlin")
        plugin("org.gradle.test-retry")
    }

    repositories {
        jcenter()
        mavenCentral()
    }

    tasks.withType<KotlinCompile>().all {
        kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.ExperimentalStdlibApi"
        kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
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

        testImplementation(project(":tests"))
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
        // that attempts to fix flaky tests once and for all
        retry {
            maxRetries.set(3)
            maxFailures.set(10)
        }
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
        create<MavenPublication>("filesystems-core") {
            from(subprojects.first { it.name == "filesystems-core" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "filesystems-core"
        }
        create<MavenPublication>("filesystems-dropbox") {
            from(subprojects.first { it.name == "filesystems-dropbox" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "filesystems-dropbox"
        }
        create<MavenPublication>("metrics-core") {
            from(subprojects.first { it.name == "metrics-core" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "metrics-core"
        }
        create<MavenPublication>("metrics-prometheus") {
            from(subprojects.first { it.name == "metrics-prometheus" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "metrics-prometheus"
        }
    }
    repositories {
        maven {
            name = "WaveBeansMavenCentral"
            url = uri("https://s01.oss.sonatype.org")
            credentials {
                username = findProperty("gpr.user")?.toString() ?: System.getenv("GPR_USER") ?: ""
                password = findProperty("gpr.key")?.toString() ?: System.getenv("GPR_TOKEN") ?: ""
            }
        }
    }
}


//bintray {
//    user = findProperty("jfrog.user")?.toString() ?: ""
//    key = findProperty("jfrog.key")?.toString() ?: ""
//    setPublications("lib", "exe", "proto", "http", "filesystems.core", "filesystems.dropbox", "metrics.core", "metrics.prometheus")
//    pkg(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.PackageConfig> {
//        repo = "wavebeans"
//        name = "wavebeans"
//        userOrg = "wavebeans"
//        vcsUrl = "https://github.com/WaveBeans/wavebeans"
//        publish = true
//        setLicenses("Apache-2.0")
//        version(delegateClosureOf<com.jfrog.bintray.gradle.BintrayExtension.VersionConfig> {
//            name = project.version.toString()
//        })
//    })
//}
