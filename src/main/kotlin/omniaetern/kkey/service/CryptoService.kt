package omniaetern.kkey.service

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoService {
    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val TAG_LENGTH = 128 // Bits

    // Encrypts a string and returns (Encrypted Data, IV)
    fun encrypt(plainText: String, secretKey: ByteArray): Pair<String, String> {
        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(secretKey, "AES"))

        val encryptedBytes = cipher.doFinal(plainText.toByteArray())
        val iv = cipher.iv

        return Base64.getEncoder().encodeToString(encryptedBytes) to
                Base64.getEncoder().encodeToString(iv)
    }

    // Decrypts data using the IV and SecretKey
    fun decrypt(encryptedBase64: String, ivBase64: String, secretKey: ByteArray): String {
        val cipher = Cipher.getInstance(ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH, Base64.getDecoder().decode(ivBase64))

        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(secretKey, "AES"), gcmSpec)

        val decodedBytes = Base64.getDecoder().decode(encryptedBase64)
        return String(cipher.doFinal(decodedBytes))
    }
}