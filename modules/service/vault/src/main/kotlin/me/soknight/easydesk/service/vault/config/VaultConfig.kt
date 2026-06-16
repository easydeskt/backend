package me.soknight.easydesk.service.vault.config

// Holds the AES-256 master key (exactly 32 bytes) decoded from the VAULT_ENCRYPTION_KEY env var.
// Created externally in the app Koin module so the key length is validated at startup.
data class VaultConfig(val encryptionKey: ByteArray) {

    init {
        require(encryptionKey.size == KEY_BYTES) {
            "VAULT_ENCRYPTION_KEY must decode to exactly $KEY_BYTES bytes (256 bits), got ${encryptionKey.size}"
        }
    }

    override fun equals(other: Any?): Boolean =
        other is VaultConfig && encryptionKey.contentEquals(other.encryptionKey)

    override fun hashCode(): Int = encryptionKey.contentHashCode()

    override fun toString(): String = "VaultConfig(encryptionKey=***)"

    companion object {
        const val KEY_BYTES = 32
    }
}
