package me.soknight.easydesk.service.channels

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.ChannelProvider
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.ConversationFactory
import me.soknight.easydesk.service.channels.data.domain.ChannelIdentity
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.channels.registry.ConversationRegistry
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ConversationRegistryTest {

    private val identityRepo = mockk<ChannelIdentityRepository>(relaxed = true)
    private val mockBrand = mockk<ChannelBrand> { every { identifier } returns "telegram" }
    private val mockChannel = mockk<Channel>(relaxed = true)
    private val mockProvider = mockk<ChannelProvider> {
        every { brand } returns mockBrand
        every { channels } returns listOf(mockChannel)
    }
    private val restoredConversation = mockk<Conversation>(relaxed = true)
    private val factory = object : ConversationFactory {
        override val brand = mockBrand
        override suspend fun restore(
            channel: Channel,
            nativeId: String,
            attributes: Attributes,
        ) = restoredConversation
    }

    private val registry = ConversationRegistry(identityRepo, listOf(factory))

    @Test
    fun `should_returnCached_when_present`() {
        val conversation = mockk<Conversation>()
        registry.register(1L, conversation)
        assertNotNull(registry.getOrNull(1L))
    }

    @Test
    fun `should_restoreFromFactory_when_notCached`() {
        coEvery { identityRepo.findById(42L) } returns mockk<ChannelIdentity>(relaxed = true) {
            every { channelProvider } returns mockProvider
            every { nativeId } returns "12345"
        }
        val result = registry.getOrNull(42L)
        assertNotNull(result)
    }

    @Test
    fun `should_returnNull_when_identityNotFound`() {
        coEvery { identityRepo.findById(99L) } returns null
        assertNull(registry.getOrNull(99L))
    }

}
