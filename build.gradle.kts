plugins {
    val kotlinVersion = "1.4.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

group = "com.trafi"
version = "2.1"

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
    implementation("com.squareup:kotlinpoet:1.7.2")
    implementation("com.github.ajalt.clikt:clikt:3.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
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
