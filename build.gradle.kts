plugins {
    val kotlinVersion = "1.3.61"
    kotlin("jvm") version kotlinVersion
    id("org.jetbrains.kotlin.plugin.serialization") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "com.trafi"
version = "1.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.14.0")
    implementation("com.squareup:kotlinpoet:1.5.0")
    implementation("com.github.ajalt:clikt:2.4.0")
    implementation("com.squareup.okhttp3:okhttp:4.3.1")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    test {
        useJUnitPlatform()
    }
    jar {
        manifest {
            attributes["Main-Class"] = "MammothKt"
        }
    }
    shadowJar {
        archiveClassifier.set("")
    }
}
