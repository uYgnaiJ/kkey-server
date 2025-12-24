package omniaetern.kkey

import io.ktor.server.application.*
import kotlinx.coroutines.async
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import omniaetern.kkey.service.ServerService

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) { json() }

    configureRouting()

    launch {
        val serverList = ServerService.loadServerAddresses().toList()

        val jobs = serverList.map { ip ->
            async {
                val success: Boolean = ServerService.fetchServer(ip)
                ip to success
            }
        }

        val results = jobs.awaitAll()

        val successfulIps = results.filter { it.second }.map { it.first }

        if (successfulIps.isNotEmpty()) {
            val message = "Successfully synchronized from: ${successfulIps.joinToString(", ")}"
            log(message)
        } else {
            log("All ${serverList.size} servers in history failed to respond.")
            ServerService.fetchServerByInput(this)
        }
    }

    log("Server app started successfully")
}



