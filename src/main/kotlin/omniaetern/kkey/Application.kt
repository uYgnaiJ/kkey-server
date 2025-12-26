package omniaetern.kkey

import io.ktor.server.application.*
import kotlinx.coroutines.async
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import omniaetern.kkey.service.AppConfig
import omniaetern.kkey.service.ServerService
import omniaetern.kkey.service.DataService

fun main(args: Array<String>) {

    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    println("----------------------------------------------")
    print("Enter encryption key: ")
    var passwordInput = readlnOrNull()?.trim() ?: ""
    println("----------------------------------------------")

    while (passwordInput.isBlank()){
        err("Key cannot be blank")
        println("----------------------------------------------")
        print("Enter encryption key to start server: ")
        passwordInput = readlnOrNull()?.trim() ?: ""
        println("----------------------------------------------")
    }

    AppConfig.initKey(passwordInput)

    install(ContentNegotiation) { json() }

    install(io.ktor.server.plugins.cors.routing.CORS) {
        anyHost()
        allowHeader(io.ktor.http.HttpHeaders.ContentType)
        allowMethod(io.ktor.http.HttpMethod.Options)
        allowMethod(io.ktor.http.HttpMethod.Get)
        allowMethod(io.ktor.http.HttpMethod.Post)
        allowMethod(io.ktor.http.HttpMethod.Put)
        allowMethod(io.ktor.http.HttpMethod.Delete)
    }

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

                    if (serverSuccess) {
                        val dataSuccess = DataService.fetchData(ip)
                        ip to true
                    } else {
                        ip to false
                    }
                }
            }

            val results = jobs.awaitAll()
            val successfulIps = results.filter { it.second }.map { it.first }
            if (successfulIps.isNotEmpty()) {
                log("Successfully synchronized from: ${successfulIps.joinToString(", ")}")
            } else {
                log("All ${serverList.size} servers in history failed to respond.")
                ServerService.fetchServerByInput(this)
            }

        }
    }

    log("Server app started successfully")
}



