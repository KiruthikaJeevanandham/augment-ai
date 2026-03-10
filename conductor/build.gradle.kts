plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("com.agentic.conductor.ConductorKt")
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // JSON & YAML Parsing
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.15.2")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.15.2")

    // HTTP Client
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-jackson:2.9.0")

    // Google Cloud & Gemini
    implementation(platform("com.google.cloud:libraries-bom:26.42.0"))
    implementation("com.google.cloud:google-cloud-aiplatform")
    implementation("com.google.cloud:google-cloud-vertexai:0.4.0")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")

    // CLI Argument Parsing
    implementation("com.github.ajalt.clikt:clikt:4.2.1")

    // Logging
    implementation("org.slf4j:slf4j-api:2.0.9")
    implementation("ch.qos.logback:logback-classic:1.4.11")

    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
