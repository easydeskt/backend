package me.soknight.easydesk.service.vault.domain

import kotlin.time.Instant

/**
 * A named secret stored encrypted in the vault.
 *
 * @param encryptedValue the AES-256-GCM ciphertext — never the plaintext value; decrypt via [me.soknight.easydesk.service.vault.encryption.VaultEncryptionService]
 */
data class VaultSecret(
    val createdAt: Instant,
    val description: String?,
    val encryptedValue: String,
    val id: Long,
    val name: String,
    val updatedAt: Instant,
)
