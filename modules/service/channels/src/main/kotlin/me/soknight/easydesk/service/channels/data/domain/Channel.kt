package me.soknight.easydesk.service.channels.data.domain

import kotlinx.serialization.json.JsonObject
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * A configured inbound/outbound communication channel.
 *
 * Channels are the platform-specific endpoints (Telegram bot, VK community, IMAP mailbox)
 * through which clients reach support. Each channel has a [brand] (e.g. `"telegram"`)
 * identifying its platform and a [config] blob with platform-specific credentials.
 *
 * @param id internal auto-generated identifier
 * @param brand platform identifier (e.g. `"telegram"`, `"vkontakte"`, `"email"`)
 * @param displayName human-readable label shown in administration
 * @param config platform-specific configuration (credentials, settings)
 * @param attributes extensible metadata for platform-specific runtime state
 * @param isEnabled when `false`, the channel does not poll or send messages
 * @param createdAt timestamp of creation
 * @param updatedAt timestamp of last configuration change
 */
data class Channel(
    val id: Long,
    val brand: String,
    val displayName: String,
    val config: JsonObject,
    val attributes: JsonObject,
    val isEnabled: Boolean,
    val createdAt: Instant = Clock.System.now(),
    val updatedAt: Instant = createdAt,
)
