@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.telegram

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.MessageThreadId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.abstracts.OptionallyFromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.service.agents.domain.Agent
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.service.channels.registry.ConversationRegistry
import me.soknight.easydesk.service.tickets.data.domain.ActorKind
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageAttachmentRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.model.Agent as SupervisorAgent
import me.soknight.easydesk.supervisor.telegram.config.TelegramSupervisorConfig
import me.soknight.easydesk.supervisor.telegram.handler.TelegramAgentReplyHandler
import me.soknight.easydesk.supervisor.telegram.registry.TelegramRelayedMessageRegistry

class TelegramAgentReplyHandlerTest {

    private val bot = mockk<TelegramBot>(relaxed = true)
    private val agentRepository = mockk<AgentRepository>(relaxed = true)
    private val conversationRegistry = mockk<ConversationRegistry>(relaxed = true)
    private val eventBus = mockk<EventBus>(relaxed = true)
    private val relayedMessageRegistry = mockk<TelegramRelayedMessageRegistry>(relaxed = true)
    private val ticketMessageAttachmentRepository = mockk<TicketMessageAttachmentRepository>(relaxed = true)
    private val ticketMessageRepository = mockk<TicketMessageRepository>(relaxed = true)
    private val ticketRepository = mockk<TicketRepository>(relaxed = true)

    private val config = mockk<TelegramSupervisorConfig>(relaxed = true) {
        every { token } returns "test-token"
    }

    private val handler = TelegramAgentReplyHandler(
        agentRepository = agentRepository,
        config = config,
        conversationRegistry = conversationRegistry,
        eventBus = eventBus,
        relayedMessageRegistry = relayedMessageRegistry,
        ticketMessageAttachmentRepository = ticketMessageAttachmentRepository,
        ticketMessageRepository = ticketMessageRepository,
        ticketRepository = ticketRepository,
    )

    private val agentId = Uuid.random()
    private val replyMessageId = MessageId(42L)
    private val conversationId = 10L
    private val ticketId = 1L
    private val userId = 999L

    private val agent = Agent(
        identifier = agentId,
        displayName = "Test Agent",
        role = SupervisorAgent.Role.OPERATOR,
    )

    private val conversation = mockk<Conversation>(relaxed = true)

    @BeforeTest
    fun setUp() {
        clearMocks(
            bot, agentRepository, conversationRegistry, eventBus,
            relayedMessageRegistry, ticketMessageAttachmentRepository,
            ticketMessageRepository, ticketRepository,
        )
        coEvery {
            relayedMessageRegistry.getOrNull(replyMessageId.long)
        } returns TelegramRelayedMessageRegistry.RelayedMessage(
            conversationId = conversationId,
            ticketId = ticketId,
        )
        coEvery {
            agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, userId.toString())
        } returns agent
        coEvery { conversationRegistry.getOrNull(conversationId) } returns conversation
        coEvery { conversation.send(any(), any<suspend MessageBuilder.() -> Unit>()) } returns mockk(relaxed = true)
        coEvery { ticketMessageRepository.create(any(), any(), any(), any(), any(), any(), any(), any()) } returns mockk(relaxed = true)
    }

    @Test
    fun `should_forwardTextMessage_when_agentSendsTextReply`() = runTest {
        val message = makeTextMessage("Hello from agent", threadId = MessageThreadId(7L))

        handler.handleAgentMessage(message, bot)

        coVerify(exactly = 1) {
            conversation.send(
                replyToNativeId = replyMessageId.long.toString(),
                block = any(),
            )
        }
    }

    @Test
    fun `should_recordTicketMessage_when_agentSendsTextReply`() = runTest {
        val message = makeTextMessage("Reply text", threadId = MessageThreadId(7L))

        handler.handleAgentMessage(message, bot)

        coVerify(exactly = 1) {
            ticketMessageRepository.create(
                ticketId = ticketId,
                nativeId = any(),
                senderKind = ActorKind.AGENT,
                senderAgentId = agentId,
                senderIdentityId = null,
                plainText = "Reply text",
                inReplyToNativeId = replyMessageId.long.toString(),
                platformTimestamp = any(),
            )
        }
    }

    @Test
    fun `should_skipMessage_when_noThreadId`() = runTest {
        val message = makeTextMessage("Hello", threadId = null)

        handler.handleAgentMessage(message, bot)

        coVerify(exactly = 0) { conversation.send(any(), any<suspend MessageBuilder.() -> Unit>()) }
    }

    @Test
    fun `should_skipMessage_when_replyToIsNotRelayed`() = runTest {
        coEvery { relayedMessageRegistry.getOrNull(any()) } returns null
        val message = makeTextMessage("Hello", threadId = MessageThreadId(7L))

        handler.handleAgentMessage(message, bot)

        coVerify(exactly = 0) { conversation.send(any(), any<suspend MessageBuilder.() -> Unit>()) }
    }

    @Test
    fun `should_skipMessage_when_agentNotFound`() = runTest {
        coEvery { agentRepository.findBySupervisorBinding(any(), any()) } returns null
        val message = makeTextMessage("Hello", threadId = MessageThreadId(7L))

        handler.handleAgentMessage(message, bot)

        coVerify(exactly = 0) { conversation.send(any(), any<suspend MessageBuilder.() -> Unit>()) }
    }

    // -------------- HELPERS ------------------------------------------------------------------------------------------

    private fun makeTextMessage(text: String, threadId: MessageThreadId?): ContentMessage<TextContent> {
        val content = mockk<TextContent>(relaxed = true) { every { this@mockk.text } returns text }
        val replyToMessage = mockk<Message>(relaxed = true) {
            every { messageId } returns replyMessageId
        }
        val userMock = mockk<dev.inmo.tgbotapi.types.chat.CommonUser>(relaxed = true) {
            every { id } returns UserId(userId)
        }
        return mockk<ContentMessage<TextContent>>(relaxed = true) {
            every { this@mockk.content } returns content
            every { this@mockk.threadId } returns threadId
            every { replyTo } returns replyToMessage
            every { (this@mockk as OptionallyFromUserMessage).from } returns userMock
        }
    }

}
