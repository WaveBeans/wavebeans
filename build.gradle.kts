import org.jetbrains.kotlin.gradle.tasks.KotlinCompile


plugins {
    val kotlinVersion: String by System.getProperties()

    kotlin("jvm") version kotlinVersion
}

allprojects {

    val spekVersion:String by System.getProperties()

    apply {
        plugin("kotlin")
    }

    repositories {
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

    dependencies {
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("reflect"))

        testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
        testRuntimeOnly("org.spekframework.spek2:spek-runner-junit5:$spekVersion")
        testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.13")
        testImplementation("com.nhaarman.mockitokotlin2:mockito-kotlin:2.2.0")

    }

    val test by tasks.getting(Test::class) {
        useJUnitPlatform {
            includeEngines("spek2")
        }
    }
}

