package omniaetern.kkey.service

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import omniaetern.kkey.err
import omniaetern.kkey.log
import omniaetern.kkey.models.IP
import java.io.File

object ServerService {
    private const val `SERVER-LIST` = "src/main/resources/data/server_list.txt"

    fun saveServerAddresses(message: String) {
        val receivedIPs = message.split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { IP.parse(it) }
            .toSet()

        val currentIPs = loadServerAddresses()
        val updatedIPs = currentIPs + receivedIPs

        val file = File(`SERVER-LIST`)
        log("Checking for file at: ${file.absolutePath}")

        val fileContent = updatedIPs.joinToString("\n") { it.address }
        file.writeText(fileContent)
    }

    fun loadServerAddresses(): Set<IP> {
        val file = File(`SERVER-LIST`)
        if (!file.exists()) return emptySet()
        return file.useLines { lines -> lines.filter { it.isNotBlank() }
            .mapNotNull { line -> IP.parse(line.trim()) }
            .toSet()
        }
    }

    suspend fun fetchServer(ip: IP): Boolean {
        val formattedHost = if (ip.address.contains(":") && !ip.address.startsWith("[")) {
            "[${ip.address}]"
        } else {
            ip.address
        }

        val url = "http://$formattedHost:9092/server-list"
        return try {
            log("Connecting to $url ...")

            val response: HttpResponse = omniaetern.kkey.httpClient.get(url)

            if (response.status != HttpStatusCode.OK) {
                err("Server ${ip.address} returned status ${response.status}")
                return false
            }

            val content = response.bodyAsText()

            val addresses = content.split(";").map { it.trim() }.filter { it.isNotEmpty() }
            if (addresses.isEmpty() || addresses.any { IP.parse(it) == null }) {
                err("Received invalid content from ${ip.address}: $content")
                return false
            }
            saveServerAddresses(content)

            log("Successfully updated servers from ${ip.address}")
            true
        } catch (e: Exception) {
            err("Failed to fetch servers from ${ip.address}: ${e.message}")
            false
        }
    }

    fun fetchServerByInput(scope: CoroutineScope) {
        println("----------------------------------------------")
        print("Enter target server IP (e.g. 192.168.1.5): ")
        val address = readlnOrNull()?.trim() ?: ""
        println("----------------------------------------------")

        val ip = IP.parse(address)
        if (ip == null) {
            err("Invalid IP")
        }else{
            scope.launch { 
                fetchServer(ip)
                DataService.fetchData(ip)
            }
        }
    }
}