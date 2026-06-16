@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.telegram

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.requests.abstracts.FileId
import dev.inmo.tgbotapi.requests.abstracts.MultipartRequest
import dev.inmo.tgbotapi.requests.abstracts.Request
import dev.inmo.tgbotapi.requests.send.SendTextMessage
import dev.inmo.tgbotapi.requests.send.media.SendDocumentData
import dev.inmo.tgbotapi.requests.send.media.SendPhotoData
import dev.inmo.tgbotapi.requests.send.media.SendStickerByFileId
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.MessageId
import dev.inmo.tgbotapi.types.MessageThreadId
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.toChatId
import io.ktor.http.ContentType
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.storage.data.service.AttachmentStorageService
import me.soknight.easydesk.service.tickets.data.domain.TicketMessageAttachment
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageAttachmentRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository
import me.soknight.easydesk.supervisor.api.model.ActorKind
import me.soknight.easydesk.supervisor.api.model.TicketMessage
import me.soknight.easydesk.supervisor.telegram.config.TelegramSupervisorConfig
import me.soknight.easydesk.supervisor.telegram.handler.TelegramMessageRelayHandler
import me.soknight.easydesk.supervisor.telegram.registry.TelegramRelayedMessageRegistry
import me.soknight.easydesk.supervisor.telegram.registry.TelegramTopicRegistry

class TelegramMessageRelayHandlerTest {

    private val attachmentStorageService = mockk<AttachmentStorageService>(relaxed = true)
    private val bot = mockk<TelegramBot>(relaxed = true)
    private val chatId: ChatIdentifier = 100500L.toChatId()
    private val threadId = MessageThreadId(42L)

    private val handler = TelegramMessageRelayHandler(
        attachmentStorageService = attachmentStorageService,
        channelIdentityRepository = mockk<ChannelIdentityRepository>(relaxed = true),
        config = mockk<TelegramSupervisorConfig>(relaxed = true),
        relayedMessageRegistry = mockk<TelegramRelayedMessageRegistry>(relaxed = true),
        ticketMessageAttachmentRepository = mockk<TicketMessageAttachmentRepository>(relaxed = true),
        ticketRepository = mockk<TicketRepository>(relaxed = true),
        topicRegistry = mockk<TelegramTopicRegistry>(relaxed = true),
    )

    private val sentMessageId = MessageId(7L)

    @BeforeTest
    fun setUp() {
        clearMocks(bot)
        coEvery { bot.execute(any<Request<*>>()) } answers {
            mockk<ContentMessage<*>>(relaxed = true) { every { messageId } returns sentMessageId }
        }
    }

    @Test
    fun `should_sendStickerWithFileId_when_stickerAttachment`() = runTest {
        val requestSlot = slot<Request<*>>()
        coEvery { bot.execute(capture(requestSlot)) } answers {
            mockk<ContentMessage<*>>(relaxed = true) { every { messageId } returns sentMessageId }
        }

        val att = makeAttachment(Attachment.Kind.STICKER, attributes = mapOf("telegram.file_id" to JsonPrimitive("sticker_abc")))

        handler.relaySingleAttachment(bot, chatId, threadId, att, "User", makeMessage(plainText = null))

        val request = requestSlot.captured
        assertIs<SendStickerByFileId>(request)
        assertEquals(FileId("sticker_abc"), request.sticker)
        assertEquals(threadId, request.threadId)
    }

    @Test
    fun `should_returnNull_when_stickerHasNoFileId`() = runTest {
        val att = makeAttachment(Attachment.Kind.STICKER, attributes = emptyMap())

        val result = handler.relaySingleAttachment(bot, chatId, threadId, att, "User", makeMessage(plainText = null))

        assertNull(result)
        coVerify(exactly = 0) { bot.execute(any<Request<*>>()) }
    }

    @Test
    fun `should_sendPhotoWithFileId_when_photoHasTelegramFileId`() = runTest {
        val requestSlot = slot<Request<*>>()
        coEvery { bot.execute(capture(requestSlot)) } answers {
            mockk<ContentMessage<*>>(relaxed = true) { every { messageId } returns sentMessageId }
        }

        val att = makeAttachment(Attachment.Kind.PHOTO, attributes = mapOf("telegram.file_id" to JsonPrimitive("photo_xyz")))

        handler.relaySingleAttachment(bot, chatId, threadId, att, "Client", makeMessage(plainText = null))

        val request = requestSlot.captured
        assertIs<SendPhotoData>(request)
        assertEquals(FileId("photo_xyz"), request.photo)
        assertEquals(threadId, request.threadId)
    }

    @Test
    fun `should_sendDocumentLinkText_when_documentHasVkPlayerUrl`() = runTest {
        val requestSlot = slot<Request<*>>()
        coEvery { bot.execute(capture(requestSlot)) } answers {
            mockk<ContentMessage<*>>(relaxed = true) { every { messageId } returns sentMessageId }
        }

        val att = makeAttachment(
            Attachment.Kind.DOCUMENT,
            fileName = "video.mp4",
            attributes = mapOf("vk.player_url" to JsonPrimitive("https://vk.com/video123")),
        )

        handler.relaySingleAttachment(bot, chatId, threadId, att, "Client", makeMessage(plainText = null))

        val request = requestSlot.captured
        assertIs<SendTextMessage>(request)
        assertEquals("📎 video.mp4 — https://vk.com/video123", request.text)
        assertEquals(threadId, request.threadId)
    }

    @Test
    fun `should_sendDocumentWithFileId_when_documentHasTelegramFileId`() = runTest {
        val requestSlot = slot<Request<*>>()
        coEvery { bot.execute(capture(requestSlot)) } answers {
            mockk<ContentMessage<*>>(relaxed = true) { every { messageId } returns sentMessageId }
        }

        val att = makeAttachment(
            Attachment.Kind.DOCUMENT,
            attributes = mapOf("telegram.file_id" to JsonPrimitive("doc_file_id")),
        )

        handler.relaySingleAttachment(bot, chatId, threadId, att, "Client", makeMessage(plainText = null))

        val request = requestSlot.captured
        assertIs<SendDocumentData>(request)
        assertIs<FileId>(request.document)
        assertEquals(FileId("doc_file_id"), request.document)
        assertEquals(threadId, request.threadId)
    }

    @Test
    fun `should_sendDocumentAsBytes_when_documentHasLocalStoragePath`() = runTest {
        val requestSlot = slot<Request<*>>()
        coEvery { bot.execute(capture(requestSlot)) } answers {
            mockk<ContentMessage<*>>(relaxed = true) { every { messageId } returns sentMessageId }
        }
        val docBytes = byteArrayOf(1, 2, 3, 4)
        every { attachmentStorageService.openSource("email/abc.pdf") } returns
            Buffer().also { it.write(docBytes) }

        val att = makeAttachment(
            Attachment.Kind.DOCUMENT,
            fileName = "report.pdf",
            attributes = mapOf("local.storage_path" to JsonPrimitive("email/abc.pdf")),
        )

        handler.relaySingleAttachment(bot, chatId, threadId, att, "Client", makeMessage(plainText = null))

        val request = requestSlot.captured
        // sendDocument(ByteArray.asMultipartFile()) wraps SendDocumentData in CommonMultipartFileRequest
        // which is internal to tgbotapi — verify via its public MultipartRequest supertype.
        assertIs<MultipartRequest<*>>(request)
    }

    @Test
    fun `should_includeCaptionWithDisplayName_when_messageHasNoText`() = runTest {
        val requestSlot = slot<Request<*>>()
        coEvery { bot.execute(capture(requestSlot)) } answers {
            mockk<ContentMessage<*>>(relaxed = true) { every { messageId } returns sentMessageId }
        }

        val att = makeAttachment(Attachment.Kind.PHOTO, attributes = mapOf("telegram.file_id" to JsonPrimitive("photo_id")))

        handler.relaySingleAttachment(bot, chatId, threadId, att, "Alice", makeMessage(plainText = null))

        val request = requestSlot.captured
        assertIs<SendPhotoData>(request)
        assertEquals("📩 [Alice]", request.text)
    }

    @Test
    fun `should_sendNullCaption_when_messageHasNonBlankText`() = runTest {
        val requestSlot = slot<Request<*>>()
        coEvery { bot.execute(capture(requestSlot)) } answers {
            mockk<ContentMessage<*>>(relaxed = true) { every { messageId } returns sentMessageId }
        }

        val att = makeAttachment(Attachment.Kind.PHOTO, attributes = mapOf("telegram.file_id" to JsonPrimitive("photo_id")))

        handler.relaySingleAttachment(bot, chatId, threadId, att, "Bob", makeMessage(plainText = "Hello world"))

        val request = requestSlot.captured
        assertIs<SendPhotoData>(request)
        // caption is null when plainText is already present (sent as a separate message)
        assertNull(request.text)
    }

    // -------------- HELPERS ------------------------------------------------------------------------------------------

    private fun makeAttachment(
        kind: Attachment.Kind,
        fileName: String = "file.bin",
        attributes: Map<String, JsonElement> = emptyMap(),
    ) = TicketMessageAttachment(
        identifier = 1L,
        messageId = 10L,
        kind = kind,
        fileName = fileName,
        contentType = ContentType.Application.OctetStream,
        fileSize = null,
        channelBrand = "telegram",
        attributes = attributes,
        createdAt = Clock.System.now(),
    )

    private fun makeMessage(plainText: String?) = object : TicketMessage {
        override val identifier: Long = 10L
        override val inReplyToNativeId: String? = null
        override val nativeId: String = "native-10"
        override val plainText: String? = plainText
        override val platformTimestamp: Instant = Instant.DISTANT_PAST
        override val senderAgentId: Uuid? = null
        override val senderIdentityId: Long = 99L
        override val senderKind: ActorKind = ActorKind.IDENTITY
        override val ticketId: Long = 1L
    }

}
