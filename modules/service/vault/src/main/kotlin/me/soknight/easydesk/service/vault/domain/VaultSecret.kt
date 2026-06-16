package me.soknight.easydesk.service.vault.domain

import kotlin.time.Instant

data class VaultSecret(
    val createdAt: Instant,
    val description: String?,
    val encryptedValue: String,
    val id: Long,
    val name: String,
    val updatedAt: Instant,
)
