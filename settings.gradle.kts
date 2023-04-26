pluginManagement {
    val kotlinVersion: String by settings
    val kotlinxBenchmark: String by settings
    plugins {
        kotlin("jvm") version kotlinVersion
    }
}

rootProject.name = "webcam-capture-kotlin"

