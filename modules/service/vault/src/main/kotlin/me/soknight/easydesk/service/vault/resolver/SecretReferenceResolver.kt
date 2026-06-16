package me.soknight.easydesk.service.vault.resolver

import me.soknight.easydesk.service.vault.encryption.VaultEncryptionService
import me.soknight.easydesk.service.vault.repository.VaultSecretRepository
import org.koin.core.annotation.Single

@Single
class SecretReferenceResolver(
    private val encryptionService: VaultEncryptionService,
    private val secretRepository: VaultSecretRepository,
) {

    // Matches $KEY_NAME (standalone dollar + uppercase pattern) but NOT ${...} (env-var brace syntax).
    private val referencePattern = Regex("""\$([A-Z][A-Z0-9_]*)""")

    /**
     * Replaces every `$KEY_NAME` token in [text] with the decrypted vault value.
     * Tokens referencing deleted or non-existent secrets are left as-is;
     * the caller (channel provider) will fail at connection time — expected behavior per spec.
     */
    suspend fun resolve(text: String): String {
        val keys = referencePattern.findAll(text).map { it.groupValues[1] }.distinct().toList()
        if (keys.isEmpty()) return text
        val decrypted = keys.associateWith { key ->
            secretRepository.findByName(key)?.let { encryptionService.decrypt(it.encryptedValue) }
        }
        return referencePattern.replace(text) { match -> decrypted[match.groupValues[1]] ?: match.value }
    }

}
