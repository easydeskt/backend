package me.soknight.easydesk.service.channels

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.ChannelProvider
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.ConversationFactory
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.channels.data.repository.ConversationRepository
import me.soknight.easydesk.service.channels.registry.ConversationRegistry

class ConversationRegistryTest {

    private val identityRepo = mockk<ChannelIdentityRepository>(relaxed = true)
    private val conversationRepo = mockk<ConversationRepository>(relaxed = true)
    private val mockBrand = mockk<ChannelBrand> { every { identifier } returns "telegram" }
    private val mockProvider = mockk<ChannelProvider> {
        every { brand } returns mockBrand
    }
    private val restoredConversation = mockk<Conversation>(relaxed = true)

    private val factory = object : ConversationFactory {
        override val brand = mockBrand
        override suspend fun restore(serviceChannelId: Long, nativeId: String, attributes: Attributes) =
            restoredConversation.takeIf { serviceChannelId == 7L && nativeId == "user_123" }
    }

    private val registry = ConversationRegistry(identityRepo, conversationRepo, listOf(factory))

    @Test
    fun `should_returnCached_when_present`() = runBlocking {
        val cached = mockk<Conversation>()
        registry.register(1L, cached)
        assertSame(cached, registry.getOrNull(1L))
    }

    @Test
    fun `should_restoreFromFactory_when_notCached`() = runBlocking {
        coEvery { conversationRepo.findById(42L) } returns mockk {
            every { channelId } returns 7L
            every { identityId } returns 100L
            every { attributes } returns JsonObject(emptyMap())
        }
        coEvery { identityRepo.findById(100L) } returns mockk(relaxed = true) {
            every { channelProvider } returns mockProvider
            every { nativeId } returns "user_123"
        }

        val result = registry.getOrNull(42L)
        assertSame(restoredConversation, result)
    }

    @Test
    fun `should_returnNull_when_conversationNotFound`() = runBlocking {
        coEvery { conversationRepo.findById(99L) } returns null
        assertNull(registry.getOrNull(99L))
    }

    @Test
    fun `should_cacheRestoredConversation_when_successfulRestore`() = runBlocking {
        coEvery { conversationRepo.findById(55L) } returns mockk {
            every { channelId } returns 7L
            every { identityId } returns 200L
            every { attributes } returns JsonObject(emptyMap())
        }
        coEvery { identityRepo.findById(200L) } returns mockk(relaxed = true) {
            every { channelProvider } returns mockProvider
            every { nativeId } returns "user_123"
        }

        registry.getOrNull(55L) // first call — restores and caches
        val second = registry.getOrNull(55L) // second call — should hit cache

        // conversationRepo should only be called once (cache hit on second call)
        coVerify(exactly = 1) { conversationRepo.findById(55L) }
        assertSame(restoredConversation, second)
    }

}
