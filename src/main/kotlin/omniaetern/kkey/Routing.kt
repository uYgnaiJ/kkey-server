package omniaetern.kkey

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import omniaetern.kkey.models.IP
import omniaetern.kkey.models.PasswordEntry
import omniaetern.kkey.service.DataService
import omniaetern.kkey.service.ServerService

fun Application.configureRouting() {
    routing {
        get("/hello") {
            call.respondText("Hello World!")
        }

        get("/server-list"){
            val servers: Set<IP> = ServerService.loadServerAddresses()
            val responseText = servers.joinToString(";")
            call.respondText(responseText)
        }

        get("/data"){
            val data: List<PasswordEntry> = DataService.loadData()
            call.respond(data)
        }
    }
}
