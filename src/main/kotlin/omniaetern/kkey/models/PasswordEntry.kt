package omniaetern.kkey.models

import kotlinx.serialization.Serializable

@Serializable
data class PasswordEntry(
    val id: String, // UUID
    val name: String,
    val password: String,
    val description: String,
    val url: String,
    val lastModified: Long,
    val version: Int = 1,
    val deleted: Boolean,
)