package omniaetern.kkey

import io.ktor.server.application.*
import kotlinx.coroutines.async
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import omniaetern.kkey.service.ServerService
import omniaetern.kkey.service.DataService

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    install(ContentNegotiation) { json() }

    configureRouting()

    monitor.subscribe(ApplicationStarted) { application ->
        application.launch {
            log("Server started, initiating synchronization...")
            delay(3000)
            
            val serverList = ServerService.loadServerAddresses().toList()
            if (serverList.isEmpty()) {
                log("Server history is empty.")
                ServerService.fetchServerByInput(this)
                return@launch
            }

            val jobs = serverList.map { ip ->
                async {
                    val serverSuccess = ServerService.fetchServer(ip)
                    val dataSuccess = DataService.fetchData(ip)
                    ip to (serverSuccess || dataSuccess)
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

            DataService.fetchData(successfulIps.first())
        }
    }

    log("Server app started successfully")
}



