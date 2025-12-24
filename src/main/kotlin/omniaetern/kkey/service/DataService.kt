package omniaetern.kkey.service

import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import omniaetern.kkey.models.IP
import omniaetern.kkey.models.PasswordEntry
import omniaetern.kkey.log
import omniaetern.kkey.err
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import java.io.File

object DataService {
    private const val PASSWORDS = "src/main/resources/data/passwords.txt"

    fun loadData(): List<PasswordEntry>{
        val file = File(PASSWORDS)
        if (!file.exists()) return emptyList()
        return file.readLines()
            .filter { it.isNotBlank() }
            .map { line -> Json.decodeFromString<PasswordEntry>(line) }
    }

    fun saveData(incomingData: List<PasswordEntry>) {
        val currentMap = loadData().associateBy { it.id }.toMutableMap()

        for (incoming in incomingData) {
            val existing = currentMap[incoming.id]
            if (existing == null) {
                // New ID: Just add it
                currentMap[incoming.id] = incoming
            } else {
                // Conflict
                val shouldUpdate = when {
                    // Rule 1: Greater version wins
                    incoming.version > existing.version -> true
                    // Rule 2: Version is same, later time wins
                    incoming.version == existing.version && incoming.lastModified > existing.lastModified -> true
                    else -> false
                }
                if (shouldUpdate) {
                    currentMap[incoming.id] = incoming
                }
            }
        }

        val file = File(PASSWORDS)
        file.parentFile?.mkdirs()
        val newFileContent = currentMap.values.joinToString("\n") { entry -> Json.encodeToString(entry) }
        file.writeText(newFileContent)
    }

    suspend fun fetchData(ip: IP): Boolean {
        val url = "http://${ip.urlSafeAddress}:9092/data"
        return try {
            log("Fetching data from $url ...")
            val response: HttpResponse = omniaetern.kkey.httpClient.get(url)

            if (response.status != HttpStatusCode.OK) {
                err("Server ${ip.address} returned status ${response.status}")
                return false
            }

            val content = response.bodyAsText()
            val incomingData: List<PasswordEntry> = Json.decodeFromString(content)
            saveData(incomingData)
            
            log("Successfully synchronized data from ${ip.address}")
            true
        } catch (e: Exception) {
            err("Failed to fetch data from ${ip.address}: ${e.message}")
            false
        }
    }

}