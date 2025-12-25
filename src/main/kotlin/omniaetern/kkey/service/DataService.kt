package omniaetern.kkey.service

import kotlinx.serialization.json.Json
import omniaetern.kkey.models.IP
import omniaetern.kkey.models.PasswordEntry
import omniaetern.kkey.log
import omniaetern.kkey.err
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import omniaetern.kkey.models.SecureRequest
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
        val now = System.currentTimeMillis()
        for (incoming in incomingData) {
            val existing = currentMap[incoming.id]
            when {
                incoming.id.isBlank() -> {
                    val newEntry = incoming.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        lastModified = now
                    )
                    currentMap[newEntry.id] = newEntry
                }
                existing == null -> {
                    currentMap[incoming.id] = incoming
                }
                else -> {
                    val shouldUpdate = when {
                        incoming.version > existing.version -> true
                        incoming.version == existing.version && incoming.lastModified > existing.lastModified -> true
                        else -> false
                    }
                    if (shouldUpdate) {
                        val updatedEntry = if (incoming.version > existing.version) {
                            incoming.copy(lastModified = maxOf(incoming.lastModified, now))
                        } else {
                            incoming
                        }
                        currentMap[incoming.id] = updatedEntry
                    }
                }
            }
        }
        val file = File(PASSWORDS)
        file.parentFile?.mkdirs()
        val newFileContent = currentMap.values.joinToString("\n") { entry ->
            Json.encodeToString(entry)
        }
        return file.writeText(newFileContent)
    }

    suspend fun fetchData(ip: IP): Boolean {
        val url = "http://${ip.urlSafeAddress}:9092/fetch/data"
        return try {
            log("Fetching data from $url ...")
            val response: HttpResponse = omniaetern.kkey.httpClient.get(url)
            if (response.status != HttpStatusCode.OK) {
                err("Server ${ip.address} returned status ${response.status}")
                return false
            }
            val content = response.bodyAsText()
            val secureWrapper = Json.decodeFromString<SecureRequest>(content)
            val decryptedJson = CryptoService.decrypt(
                secureWrapper.encryptedData,
                secureWrapper.iv,
                AppConfig.secretKey
            )
            val incomingData: List<PasswordEntry> = Json.decodeFromString(decryptedJson)

            saveData(incomingData)

            log("Successfully synchronized data from ${ip.address}")
            true
        } catch (e: Exception) {
            err("Failed to fetch data from ${ip.address}: ${e.message}")
            false
        }
    }

}