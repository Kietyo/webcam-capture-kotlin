import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
    application
}

group = "com.github.sarxos.webcam"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.3")
    implementation("com.nativelibs4java:bridj:0.7.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

//application {
//    mainClass.set("MainKt")
//}