import com.google.protobuf.gradle.*

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath(libs.protobuf.gradle)
    }
}

repositories {
    google()
    mavenCentral()
}

plugins {
    idea
    alias(libs.plugins.osdetector)
    alias(libs.plugins.protobuf)
}

idea {
    module {
        sourceDirs.add(file("${projectDir}/src/main/proto"))
    }
}

dependencies {
    api(libs.protobuf.java)
    api(libs.grpc.protobuf)
    api(libs.protobuf.java.util)
    api(libs.grpc.stub)
    implementation(libs.grpc.netty.shaded)
    compileOnly(libs.javax.annotation.api)
}

tasks.withType<Javadoc> {
    options {
        this as StandardJavadocDocletOptions
        addBooleanOption("Xdoclint:none", true)
        quiet()
    }
}

protobuf {
    plugins {
        id("grpc") {
            val grpcVer = libs.versions.grpc.get()
            artifact = if (osdetector.os == "osx") {
                "io.grpc:protoc-gen-grpc-java:${grpcVer}:osx-x86_64"
            } else {
                "io.grpc:protoc-gen-grpc-java:${grpcVer}"
            }
        }
    }
    protoc {
        val protobufVer = libs.versions.protobuf.get()
        artifact = if (osdetector.os == "osx") {
            "com.google.protobuf:protoc:${protobufVer}:osx-x86_64"
        } else {
            "com.google.protobuf:protoc:${protobufVer}"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}
