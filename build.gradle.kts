import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    val kotlinVersion: String by System.getProperties()

    kotlin("jvm") version kotlinVersion
    id("org.gradle.test-retry") version "1.2.0"

    `java-library`
    `maven-publish`
    signing
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
            populatePom(
                "WaveBeans Lib",
                "WaveBeans API library. Provides the way to define streams and basic execution functionality."
            )
        }
        create<MavenPublication>("exe") {
            from(subprojects.first { it.name == "exe" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "exe"
            populatePom(
                "WaveBeans Exe",
                "WaveBeans Execution environment. Provides the way to execute streams in different modes."
            )
        }
        create<MavenPublication>("proto") {
            from(subprojects.first { it.name == "proto" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "proto"
            populatePom(
                "WaveBeans Proto",
                "WaveBeans Protobuf files and clients. For internal use only."
            )
        }
        create<MavenPublication>("http") {
            from(subprojects.first { it.name == "http" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "http"
            populatePom(
                "WaveBeans HTTP",
                "WaveBeans HTTP server. Provides the way to access functionality over HTTP Restful API."
            )
        }
        create<MavenPublication>("filesystems-core") {
            from(subprojects.first { it.name == "filesystems-core" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "filesystems-core"
            populatePom(
                "WaveBeans FileSystems Core",
                "WaveBeans subsystem to provide file specific operations."
            )
        }
        create<MavenPublication>("filesystems-dropbox") {
            from(subprojects.first { it.name == "filesystems-dropbox" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "filesystems-dropbox"
            populatePom(
                "WaveBeans DropBox FileSystem",
                "FileSystem implementation to access file in DropBox account."
            )
        }
        create<MavenPublication>("metrics-core") {
            from(subprojects.first { it.name == "metrics-core" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "metrics-core"
            populatePom(
                "WaveBeans Metrics Core",
                "WaveBeans monitoring subsystem to emit and collect metrics during execution."
            )
        }
        create<MavenPublication>("metrics-prometheus") {
            from(subprojects.first { it.name == "metrics-prometheus" }.components["java"])
            groupId = "io.wavebeans"
            artifactId = "metrics-prometheus"
            populatePom(
                "WaveBeans Prometheus Metrics",
                "WaveBeans monitoring subsystem Implementation to emit metrics to Prometheus."
            )
        }
    }
    repositories {
        maven {
            name = "WaveBeansMavenCentral"
            url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = findProperty("maven.user")?.toString() ?: ""
                password = findProperty("maven.key")?.toString() ?: ""
            }
        }
    }
}

signing {
    sign(publishing.publications)
}

fun MavenPublication.populatePom(
    nameValue: String,
    descriptionValue: String
) {
    pom {
        name.set(nameValue)
        description.set(descriptionValue)
        url.set("https://wavebeans.io")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        scm {
            url.set("https://github.com/WaveBeans/wavebeans")
            connection.set("scm:git:git://github.com:WaveBeans/wavebeans.git")
            developerConnection.set("scm:git:ssh://github.com:WaveBeans/wavebeans.git")
        }
        developers {
            developer {
                name.set("Alexey Subbotin")
                url.set("https://github.com/asubb")
            }
        }
    }
}