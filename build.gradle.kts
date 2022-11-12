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
//    testImplementation("junit:junit:4.13.1")
    testImplementation("org.assertj:assertj-core:1.6.1")
//    testImplementation("org.easymock:easymock:3.2")
//    testImplementation("org.easymock:easymock:5.0.1")
//    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    testImplementation("io.mockk:mockk:1.9.3")
    testImplementation("junit:junit:4.13.1")
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