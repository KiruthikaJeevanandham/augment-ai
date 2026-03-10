plugins {
    kotlin("jvm") version "1.9.21" apply false
}

allprojects {
    group = "com.agentic.conductor"
    version = "1.0.0"

    repositories {
        mavenCentral()
        google()
    }
}

