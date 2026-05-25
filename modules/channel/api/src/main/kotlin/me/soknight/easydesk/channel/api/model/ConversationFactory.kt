package me.soknight.easydesk.channel.api.model

import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.dsl.Attributes

/**
 * Reconstructs persisted conversations after a bot restart.
 *
 * Each [Channel] implementation provides a [ConversationFactory] that
 * restores an in-memory [Conversation] from platform-specific identity data
 * stored in the database, without requiring any API calls to the platform.
 *
 * This allows agent replies to be delivered immediately after the bot restarts,
 * even if the [ConversationRegistry] was lost.
 *
 * @see ConversationRegistry
 * @see Conversation
 */
interface ConversationFactory {

    /** The platform identity this factory reconstructs conversations for. */
    val brand: ChannelBrand

    /**
     * Reconstructs a live [Conversation] from persisted identity data.
     *
     * @param channel the channel this conversation belongs to
     * @param nativeId platform-specific user identifier
     *     (Telegram chat_id, email address, VK peer_id)
     * @param attributes stored conversation attributes (e.g. email subject)
     * @return a restored [Conversation], or null if reconstruction is not possible
     */
    suspend fun restore(
        channel: Channel,
        nativeId: String,
        attributes: Attributes,
    ): Conversation?

}
