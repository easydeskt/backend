package me.soknight.easydesk.service.vault.encryption

import me.soknight.easydesk.service.vault.config.VaultConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals

class VaultEncryptionServiceTest {

    private val testKey = ByteArray(32) { it.toByte() }
    private val config = VaultConfig(testKey)
    private val service = VaultEncryptionService(config)

    @Test
    fun `should_encrypt_and_decrypt_roundtrip`() {
        val plaintext = "super-secret-value"
        val encrypted = service.encrypt(plaintext)
        val decrypted = service.decrypt(encrypted)
        assertEquals(plaintext, decrypted)
    }

    @Test
    fun `should_produce_different_ciphertext_for_same_plaintext`() {
        val plaintext = "same-value"
        val first = service.encrypt(plaintext)
        val second = service.encrypt(plaintext)
        // Random IV per call guarantees different output even for the same plaintext.
        assertNotEquals(first, second)
    }

    @Test
    fun `should_throw_on_tampered_ciphertext`() {
        val encrypted = service.encrypt("real-value")
        val tampered = encrypted.dropLast(4) + "AAAA"
        assertFailsWith<Exception> { service.decrypt(tampered) }
    }

    @Test
    fun `should_throw_on_key_shorter_than_32_bytes`() {
        assertFailsWith<IllegalArgumentException> { VaultConfig(ByteArray(16)) }
    }

    @Test
    fun `should_throw_on_key_longer_than_32_bytes`() {
        assertFailsWith<IllegalArgumentException> { VaultConfig(ByteArray(64)) }
    }
}
