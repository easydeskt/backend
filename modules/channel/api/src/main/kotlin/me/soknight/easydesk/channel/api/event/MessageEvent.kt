package me.soknight.easydesk.channel.api.event

import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.model.Message

/**
 * A [ChannelEvent] related to a message within a conversation.
 *
 * All message events extend [ChannelEvent], inheriting the [conversation][ChannelEvent.conversation]
 * and [timestamp][ChannelEvent.timestamp]. Subtypes that carry the full [Message]
 * object implement [MessageScoped]; [Deleted] only carries the native ID
 * since the message content is no longer available.
 *
 * @see ChannelEvent
 * @see Message
 */
sealed interface MessageEvent : ChannelEvent {

    /**
     * A message has been deleted from the conversation.
     *
     * Only the platform-native ID is available — the message content
     * is no longer accessible from the platform.
     */
    interface Deleted : MessageEvent {

        /** Platform-native identifier of the deleted message. */
        val nativeId: String

    }

    /**
     * An existing message has been edited by its author.
     *
     * The [message] contains the updated content after the edit.
     *
     * @property actor the actor who performed the edit
     */
    interface Edited : MessageScoped {

        val actor: ChannelActor

    }

    /**
     * A [MessageEvent] that carries the full [Message] object.
     *
     * @see Received
     * @see Edited
     * @see Sent
     */
    sealed interface MessageScoped : MessageEvent {

        /** The message associated with this event. */
        val message: Message

    }

    /** A new message has been received from a user through the conversation. */
    interface Received : MessageScoped

    /** A message has been sent by the system through the conversation. */
    interface Sent : MessageScoped

}
