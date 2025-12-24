package omniaetern.kkey

import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*

val httpClient = HttpClient(Apache) {
    install(ContentNegotiation) {
        json()
    }
}

fun log(info: String) {
    println("=== inf ===: $info")
}

fun err(message: String) {
    println("=== err ===: $message")
}