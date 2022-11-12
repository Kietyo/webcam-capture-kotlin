import org.jetbrains.kotlin.com.intellij.openapi.vfs.StandardFileSystems.jar
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
//    application
}

group = "com.github.sarxos.webcam"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.slf4j:slf4j-api:2.0.3")
//    implementation("com.nativelibs4java:bridj:0.7.0")
    implementation("com.nativelibs4java:bridj:0.7.0")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

//jar {
//    manifest {
//        attributes ...
//    }
//    // This line of code recursively collects and copies all of a project's files
//    // and adds them to the JAR itself. One can extend this task, to skip certain
//    // files or particular types at will
//    from { configurations.compileClasspath.collect { it.isDirectory() ? it : zipTree(it) } }
//}

//application {
//    mainClass.set("MainKt")
//}