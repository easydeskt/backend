package me.soknight.easydesk.channel.api

import me.soknight.easydesk.channel.api.model.Conversation

/**
 * A named connection to a messaging platform.
 *
 * Represents a specific bot, inbox, or community account configured
 * under a [ChannelProvider]. Each channel corresponds to a single
 * profile in the provider's YAML config file.
 *
 * Per-user dialogs within this channel are modeled as [Conversation]s.
 *
 * ```
 * ChannelProvider (Telegram)
 *   └── Channel ("support-bot", token = ...)
 *         ├── Conversation (user A)
 *         └── Conversation (user B)
 * ```
 *
 * @see ChannelProvider
 * @see Conversation
 */
interface Channel {

    /** Unique machine-readable key from the YAML config profile (e.g., `"support-bot"`). */
    val identifier: String

    /** Human-readable name shown in the UI. Defaults to [identifier]. */
    val humanName: String
        get() = identifier

    /** The provider that created this channel. */
    val provider: ChannelProvider

    /** Platform-specific configuration for this channel. */
    val config: ChannelConfig

}
