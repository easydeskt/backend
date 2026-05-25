package me.soknight.easydesk.channel.vkontakte

import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.ConversationFactory
import org.koin.core.annotation.Single

/**
 * Reconstructs [VKontakteConversation] instances from persisted identity data after a restart.
 *
 * @see ConversationFactory
 */
@Single
class VKontakteConversationFactory : ConversationFactory {

    override val brand: ChannelBrand get() = VKontakteBrand

    override suspend fun restore(
        serviceChannelId: Long,
        nativeId: String,
        attributes: Attributes,
    ): Conversation? {
        val channel = VKontakteProvider.getChannel(serviceChannelId) ?: return null
        val bot = VKontakteProvider.getBot(serviceChannelId) ?: return null
        return VKontakteConversation(
            attributes = attributes,
            bot = bot,
            channel = channel,
            peerId = nativeId.toLongOrNull() ?: return null,
        )
    }

}
