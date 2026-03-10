package com.example.plugins

import com.example.model.VersionResponse
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import java.util.Properties

fun Application.configureRouting() {

    val gitProperties = Properties().apply {
        javaClass.classLoader.getResourceAsStream("git.properties")?.use { load(it) }
    }
    val gitCommit = gitProperties.getProperty("git.commit.id.abbrev", "unknown")

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        get("/version") {
            val version = environment.config.property("ktor.application.version").getString()
            call.respond(VersionResponse(version = version, gitCommit = gitCommit))
        }
    }
}
