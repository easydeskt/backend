package me.soknight.easydesk.service.vault.encryption

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import me.soknight.easydesk.service.vault.config.VaultConfig
import org.koin.core.annotation.Provided
import org.koin.core.annotation.Single

// AES-256-GCM: authenticated encryption with a unique random 12-byte IV per secret.
// Stored format: Base64(IV || ciphertext+tag). The 32-byte master key never touches the DB.
@Single
class VaultEncryptionService(@Provided private val config: VaultConfig) {

    private val secureRandom = SecureRandom()

    fun encrypt(plaintext: String): String {
        val iv = ByteArray(IV_BYTES).also(secureRandom::nextBytes)
        val ciphertext = buildCipher(Cipher.ENCRYPT_MODE, iv).doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + ciphertext)
    }

    fun decrypt(stored: String): String {
        val raw = Base64.getDecoder().decode(stored)
        val iv = raw.copyOfRange(0, IV_BYTES)
        val ciphertext = raw.copyOfRange(IV_BYTES, raw.size)
        return buildCipher(Cipher.DECRYPT_MODE, iv).doFinal(ciphertext).toString(Charsets.UTF_8)
    }

    private fun buildCipher(mode: Int, iv: ByteArray): Cipher =
        Cipher.getInstance(ALGORITHM).also { cipher ->
            cipher.init(mode, SecretKeySpec(config.encryptionKey, KEY_ALGORITHM), GCMParameterSpec(TAG_BITS, iv))
        }

    private companion object {
        const val ALGORITHM = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val KEY_ALGORITHM = "AES"
        const val TAG_BITS = 128
    }
}
