package omniaetern.kkey.service

import omniaetern.kkey.log

object AppConfig {
    // initialize this before the server starts
    lateinit var secretKey: ByteArray
        private set

    fun initKey(password: String) {
        // Use SHA-256 to turn a password of ANY length into a perfect 32-byte key
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        secretKey = digest.digest(password.toByteArray())

        log("Key initialized! (Key fingerprint: ${secretKey.take(4).joinToString("")})")
    }
}