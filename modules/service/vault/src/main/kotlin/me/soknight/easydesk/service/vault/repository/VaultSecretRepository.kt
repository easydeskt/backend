package me.soknight.easydesk.service.vault.repository

import me.soknight.easydesk.service.vault.domain.VaultSecret

/**
 * Persistence contract for [VaultSecret] management.
 *
 * All methods are self-contained suspended transactions. Values are stored encrypted;
 * callers encrypt before [create]/[update] and decrypt after retrieval
 * via [VaultEncryptionService][me.soknight.easydesk.service.vault.encryption.VaultEncryptionService].
 */
interface VaultSecretRepository {

    /** Creates a new secret. Returns `null` when [name] already exists (unique conflict). */
    suspend fun create(name: String, description: String?, encryptedValue: String): VaultSecret?

    /** Deletes the secret with [id]. Returns `false` when not found. */
    suspend fun delete(id: Long): Boolean

    /** Returns all secrets ordered by name ascending. */
    suspend fun findAll(): List<VaultSecret>

    /** Returns the secret with [id], or `null` when not found. */
    suspend fun findById(id: Long): VaultSecret?

    /**
     * Returns the secret with [name], or `null` when not found.
     * Used by [SecretReferenceResolver][me.soknight.easydesk.service.vault.resolver.SecretReferenceResolver].
     */
    suspend fun findByName(name: String): VaultSecret?

    /**
     * Updates mutable fields. Returns `null` when not found.
     *
     * @param description non-null updates the description; blank string clears it to `null`; `null` leaves unchanged
     * @param encryptedValue non-null replaces the stored encrypted value; `null` leaves unchanged
     */
    suspend fun update(id: Long, description: String?, encryptedValue: String?): VaultSecret?

}
