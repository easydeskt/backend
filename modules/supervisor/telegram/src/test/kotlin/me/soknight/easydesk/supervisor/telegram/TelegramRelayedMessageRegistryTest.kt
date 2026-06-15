package me.soknight.easydesk.supervisor.telegram

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import me.soknight.easydesk.supervisor.telegram.registry.TelegramRelayedMessageRegistry

class TelegramRelayedMessageRegistryTest {

    private val registry = TelegramRelayedMessageRegistry()

    @Test
    fun `should_allowNullClientNativeId_when_registered`() {
        registry.register(
            supervisorMessageId = 55L,
            conversationId = 2L,
            ticketId = 20L,
            clientNativeId = null,
        )

        val result = registry.getOrNull(55L)

        assertNull(result?.clientNativeId)
    }

    @Test
    fun `should_preserveClientNativeId_when_registered`() {
        registry.register(
            supervisorMessageId = 42L,
            conversationId = 1L,
            ticketId = 10L,
            clientNativeId = "client-msg-99",
        )

        val result = registry.getOrNull(42L)

        assertEquals("client-msg-99", result?.clientNativeId)
        assertEquals(1L, result?.conversationId)
        assertEquals(10L, result?.ticketId)
    }

}
