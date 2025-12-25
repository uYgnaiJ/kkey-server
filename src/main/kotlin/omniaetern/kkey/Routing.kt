package omniaetern.kkey

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.*
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import omniaetern.kkey.models.IP
import omniaetern.kkey.models.PasswordEntry
import omniaetern.kkey.service.DataService
import omniaetern.kkey.service.ServerService
import kotlinx.serialization.json.Json
import omniaetern.kkey.models.SecureRequest
import omniaetern.kkey.service.*

fun Application.configureRouting() {
    routing {
        get("/hello") {
            call.respondText("Hello World!")
        }
        get("/fetch/server-list"){
            log("Received request for /server-list")
            val servers: Set<IP> = ServerService.loadServerAddresses()
            val responseText = servers.joinToString(";") { it.address }
            call.respondText(responseText)
        }

        get("/fetch/data"){
            log("Received request for /data")

            // 1. Get the plain data
            val plainList: List<PasswordEntry> = DataService.loadData()

            // 2. Convert to JSON String
            val jsonString = Json.encodeToString(plainList)

            // 3. Encrypt it using the Global Key
            val (encryptedData, iv) = CryptoService.encrypt(jsonString, AppConfig.secretKey)

            // 4. Send the wrapper
            // Note: SecureRequest MUST be @Serializable and accessible here
            call.respond(SecureRequest(encryptedData = encryptedData, iv = iv))
        }

        post("/update"){
            try {
                // 1. Receive the wrapper
                val secureReq = call.receive<SecureRequest>()

                // 2. Decrypt it
                val decryptedJson = CryptoService.decrypt(
                    secureReq.encryptedData,
                    secureReq.iv,
                    AppConfig.secretKey
                )

                // 3. Turn JSON back into Objects
                val passwordEntries = Json.decodeFromString<List<PasswordEntry>>(decryptedJson)

                // 4. Process Logic
                DataService.saveData(passwordEntries)

                call.respond(HttpStatusCode.OK, "Synced successfully")

            } catch (e: Exception) {
                // This catches bad passwords, tampering, or malformed JSON
                call.respond(HttpStatusCode.BadRequest, "Decryption Error: ${e.message}")
            }
        }
    }
}

