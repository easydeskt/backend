package me.soknight.easydesk.channel.email

import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.ConversationFactory
import me.soknight.easydesk.channel.email.domain.EmailConversation
import org.koin.core.annotation.Single

/**
 * Reconstructs [EmailConversation] instances from persisted identity data after a restart.
 *
 * @see ConversationFactory
 */
@Single
class EmailConversationFactory : ConversationFactory {

    override val brand: ChannelBrand get() = EmailBrand

    override suspend fun restore(
        serviceChannelId: Long,
        nativeId: String,
        attributes: Attributes,
    ): Conversation? {
        val channel = EmailProvider.getChannel(serviceChannelId) ?: return null
        return EmailConversation(
            attributes = attributes,
            channel = channel,
            recipientAddress = nativeId,
        )
    }

}
