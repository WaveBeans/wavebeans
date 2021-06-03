import com.google.protobuf.gradle.*

val grpcVersion: String by System.getProperties()
val protobufVersion: String by System.getProperties()

buildscript {
    val protobufGradlePluginVersion: String by System.getProperties()

    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:$protobufGradlePluginVersion")
    }
}

repositories {
    google()
    mavenCentral()
}

plugins {
    idea
}

apply {
    plugin("com.google.protobuf")
}

idea {
    module {
        sourceDirs.add(file("${projectDir}/src/main/proto"))
    }
}

dependencies {
    api("com.google.protobuf:protobuf-java:$protobufVersion")
    api("io.grpc:grpc-protobuf:$grpcVersion")
    api("com.google.protobuf:protobuf-java-util:$protobufVersion")
    api("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    compileOnly("javax.annotation:javax.annotation-api:1.3.1")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:$protobufVersion" }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
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