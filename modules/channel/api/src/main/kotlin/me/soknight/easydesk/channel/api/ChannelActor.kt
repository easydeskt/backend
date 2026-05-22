@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.channel.api

import me.soknight.easydesk.channel.api.model.Message
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Represents a participant in channel communication.
 *
 * Every [message][Message] has a [Message.sender]
 * and a [Message.receiver], each of which is a [ChannelActor]. The four variants cover
 * all possible participants:
 * - [Identity] — a real user on the messaging platform (customer)
 * - [Agent] — a helpdesk agent acting through the system
 * - [System] — the EasyDesk system itself (e.g., automated replies)
 * - [Unknown] — an unidentifiable actor (e.g., deleted account)
 *
 * @see me.soknight.easydesk.channel.api.model.Message
 */
sealed interface ChannelActor {

    /** Human-readable representation of this actor for logging and display. */
    val humanName: String

    /**
     * A helpdesk agent acting through the system.
     *
     * @property agentId internal UUID of the agent within EasyDesk
     */
    interface Agent : ChannelActor {

        val agentId: Uuid

        override val humanName: String
            get() = "easydesk@$agentId"

    }

    /**
     * A real user on the messaging platform (typically a customer).
     *
     * The identity is tied to a specific platform (e.g., Telegram user ID,
     * VK user ID, email address) but not to a specific [Channel] connection.
     *
     * @property channelBrand the platform brand this identity belongs to, derived from [channelProvider]
     * @property channelProvider the provider that manages this identity's platform
     * @property nativeId platform-specific user identifier (e.g., Telegram user ID, email address)
     */
    interface Identity : ChannelActor {

        val channelBrand: ChannelBrand
            get() = channelProvider.brand

        val channelProvider: ChannelProvider

        val nativeId: String

        override val humanName: String
            get() = "${channelBrand.identifier}@$nativeId"

    }

    /** The EasyDesk system itself (e.g., automated or system-generated messages). */
    data object System : ChannelActor {

        override val humanName: String
            get() = "system"

    }

    /** An unidentifiable actor (e.g., a deleted account or missing sender info). */
    data object Unknown : ChannelActor {

        override val humanName: String
            get() = "unknown"

    }

}