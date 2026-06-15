@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.telegram

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.MessageThreadId
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.UserId
import dev.inmo.tgbotapi.types.message.abstracts.CommonForumContentMessage
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.Message
import dev.inmo.tgbotapi.types.message.abstracts.OptionallyFromUserMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import io.ktor.http.ContentType
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.telegram.TelegramAttachment
import me.soknight.easydesk.channel.telegram.TelegramAttachmentParser
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.service.agents.domain.Agent
import me.soknight.easydesk.service.agents.repository.AgentRepository
import me.soknight.easydesk.service.channels.registry.ConversationRegistry
import me.soknight.easydesk.service.tickets.data.domain.ActorKind
import me.soknight.easydesk.service.tickets.data.domain.TicketMessage
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageAttachmentRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.model.Agent as SupervisorAgent
import me.soknight.easydesk.supervisor.telegram.config.TelegramSupervisorConfig
import me.soknight.easydesk.supervisor.telegram.handler.TelegramAgentReplyHandler
import me.soknight.easydesk.supervisor.telegram.registry.TelegramRelayedMessageRegistry

class TelegramAgentReplyHandlerTest {

    // Created BEFORE bot to prime Kotlin reflection cache for TicketMessage before BusinessChatId is instrumented,
    // avoiding MockK's recording-state loop triggered by getOrCreateKotlinClass + getChatIdWithBusinessConnectionId.
    private val ticketMessageResult = mockk<TicketMessage>(relaxed = true)

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
        mockkObject(TelegramAttachmentParser)
        clearMocks(
            bot, agentRepository, conversationRegistry, eventBus,
            relayedMessageRegistry, ticketMessageAttachmentRepository,
            ticketMessageRepository, ticketMessageResult, ticketRepository,
        )
        coEvery { TelegramAttachmentParser.parse(any(), any(), any()) } returns emptyList()
        every {
            relayedMessageRegistry.getOrNull(replyMessageId.long)
        } returns TelegramRelayedMessageRegistry.RelayedMessage(
            clientNativeId = "client-native-42",
            conversationId = conversationId,
            ticketId = ticketId,
        )
        coEvery {
            agentRepository.findBySupervisorBinding(TelegramSupervisorBrand, userId.toString())
        } returns agent
        coEvery { conversationRegistry.getOrNull(conversationId) } returns conversation
        coEvery { conversation.send(any<String>(), any<MessageBuilder.() -> Unit>()) } returns mockk(relaxed = true)
    }

    @AfterTest
    fun tearDown() {
        unmockkObject(TelegramAttachmentParser)
    }

    @Test
    fun `should_forwardTextMessage_when_agentSendsTextReply`() = runTest {
        val message = makeThreadedMessage("Hello from agent")

        handler.handleAgentMessage(message, bot)

        coVerify(exactly = 1) {
            conversation.send(
                replyToNativeId = "client-native-42",
                block = any(),
            )
        }
    }

    @Test
    fun `should_recordTicketMessage_when_agentSendsTextReply`() {
        var capturedTicketId: Long? = null
        var capturedSenderKind: ActorKind? = null
        var capturedSenderAgentId: Uuid? = null
        var capturedPlainText: String? = null
        var capturedInReplyTo: String? = null
        // Delegation wrapper avoids coEvery recording state entirely, preventing MockK's
        // BusinessChatId value-class reflection loop triggered by anyValue(TicketMessage::class).
        val capturingRepo = object : TicketMessageRepository by ticketMessageRepository {
            override suspend fun create(
                ticketId: Long,
                nativeId: String,
                senderKind: ActorKind,
                senderAgentId: Uuid?,
                senderIdentityId: Long?,
                plainText: String?,
                inReplyToNativeId: String?,
                platformTimestamp: Instant,
                attributes: JsonObject,
            ): TicketMessage {
                capturedTicketId = ticketId
                capturedSenderKind = senderKind
                capturedSenderAgentId = senderAgentId
                capturedPlainText = plainText
                capturedInReplyTo = inReplyToNativeId
                return ticketMessageResult
            }
        }
        val localHandler = TelegramAgentReplyHandler(
            agentRepository = agentRepository,
            config = config,
            conversationRegistry = conversationRegistry,
            eventBus = eventBus,
            relayedMessageRegistry = relayedMessageRegistry,
            ticketMessageAttachmentRepository = ticketMessageAttachmentRepository,
            ticketMessageRepository = capturingRepo,
            ticketRepository = ticketRepository,
        )
        runTest {
            val message = makeThreadedMessage("Reply text")
            localHandler.handleAgentMessage(message, bot)
        }
        assertEquals(ticketId, capturedTicketId)
        assertEquals(ActorKind.AGENT, capturedSenderKind)
        assertEquals(agentId, capturedSenderAgentId)
        assertEquals("Reply text", capturedPlainText)
        assertEquals(replyMessageId.long.toString(), capturedInReplyTo)
    }

    @Test
    fun `should_skipMessage_when_noThreadId`() = runTest {
        val message = makeNonThreadedMessage("Hello")

        handler.handleAgentMessage(message, bot)

        coVerify(exactly = 0) { conversation.send(any<String>(), any<MessageBuilder.() -> Unit>()) }
    }

    @Test
    fun `should_skipMessage_when_replyToIsNotRelayed`() = runTest {
        every { relayedMessageRegistry.getOrNull(any()) } returns null
        val message = makeThreadedMessage("Hello")

        handler.handleAgentMessage(message, bot)

        coVerify(exactly = 0) { conversation.send(any<String>(), any<MessageBuilder.() -> Unit>()) }
    }

    @Test
    fun `should_skipMessage_when_agentNotFound`() {
        coEvery { agentRepository.findBySupervisorBinding(any(), any()) } returns null
        runTest {
            val message = makeThreadedMessage("Hello")

            handler.handleAgentMessage(message, bot)

            coVerify(exactly = 0) { conversation.send(any<String>(), any<MessageBuilder.() -> Unit>()) }
        }
    }

    @Test
    fun `should_skipMessage_when_textIsBlankAndNoAttachments`() = runTest {
        val message = makeThreadedMessage("   ")

        handler.handleAgentMessage(message, bot)

        coVerify(exactly = 0) { conversation.send(any<String>(), any<MessageBuilder.() -> Unit>()) }
    }

    @Test
    fun `should_forwardMessageAndPersistAttachment_when_attachmentsPresent`() {
        val photoAttachment = TelegramAttachment.Photo(
            fileId = "file-123",
            bytes = byteArrayOf(1, 2, 3),
            fileSize = 3L,
            height = 100,
            width = 200,
            channel = handler.supervisorChannel,
        )
        coEvery { TelegramAttachmentParser.parse(any(), any(), any()) } returns listOf(photoAttachment)
        runTest {
            val message = makeThreadedMessage("Caption text")

            handler.handleAgentMessage(message, bot)

            coVerify(exactly = 1) { conversation.send(replyToNativeId = "client-native-42", block = any()) }
            coVerify(exactly = 1) {
                ticketMessageAttachmentRepository.create(
                    messageId = any(),
                    kind = Attachment.Kind.PHOTO,
                    fileName = "photo.jpg",
                    contentType = ContentType.Image.JPEG,
                    fileSize = 3L,
                    channelBrand = any(),
                    attributes = any(),
                )
            }
        }
    }

    // -------------- HELPERS ------------------------------------------------------------------------------------------

    /**
     * Creates a mock forum content message (has a non-null threadId, triggering the relay path).
     * Uses [CommonForumContentMessage] so that [threadId] is a direct member MockK can intercept,
     * avoiding the need for mockkStatic on the threadIdOrNull extension.
     */
    private fun makeThreadedMessage(text: String): CommonForumContentMessage<TextContent> {
        val content = mockk<TextContent>(relaxed = true) { every { this@mockk.text } returns text }
        val replyToMessage = mockk<Message>(relaxed = true) {
            every { messageId } returns replyMessageId
        }
        val userMock = mockk<dev.inmo.tgbotapi.types.chat.CommonUser>(relaxed = true) {
            every { id } returns UserId(RawChatId(userId))
        }
        return mockk<CommonForumContentMessage<TextContent>>(relaxed = true) {
            every { this@mockk.content } returns content
            every { this@mockk.threadId } returns MessageThreadId(7L)
            every { replyTo } returns replyToMessage
            every { (this@mockk as OptionallyFromUserMessage).from } returns userMock
        }
    }

    /**
     * Creates a mock without a thread (threadIdOrNull returns null), triggering the early-return guard.
     */
    private fun makeNonThreadedMessage(text: String): CommonMessage<TextContent> {
        val content = mockk<TextContent>(relaxed = true) { every { this@mockk.text } returns text }
        return mockk<CommonMessage<TextContent>>(relaxed = true) {
            every { this@mockk.content } returns content
        }
    }

}
