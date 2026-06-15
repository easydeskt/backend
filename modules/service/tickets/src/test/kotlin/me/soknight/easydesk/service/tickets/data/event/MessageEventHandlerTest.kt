package me.soknight.easydesk.service.tickets.data.event

import io.ktor.http.ContentType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.core.event.EventBus
import me.soknight.easydesk.service.channels.data.repository.ChannelIdentityRepository
import me.soknight.easydesk.service.channels.data.repository.ChannelRepository
import me.soknight.easydesk.service.channels.data.repository.ConversationRepository
import me.soknight.easydesk.service.channels.registry.ConversationRegistry
import me.soknight.easydesk.service.storage.data.service.AttachmentStorageService
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageAttachmentRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketMessageRepository
import me.soknight.easydesk.service.tickets.data.repository.TicketRepository

class MessageEventHandlerTest {

    private val attachmentStorageService = mockk<AttachmentStorageService>(relaxed = true)
    private val channelIdentityRepository = mockk<ChannelIdentityRepository>(relaxed = true)
    private val channelRepository = mockk<ChannelRepository>(relaxed = true)
    private val conversationRegistry = mockk<ConversationRegistry>(relaxed = true)
    private val conversationRepository = mockk<ConversationRepository>(relaxed = true)
    private val eventBus = mockk<EventBus>(relaxed = true)
    private val ticketMessageAttachmentRepository = mockk<TicketMessageAttachmentRepository>(relaxed = true)
    private val ticketMessageRepository = mockk<TicketMessageRepository>(relaxed = true)
    private val ticketRepository = mockk<TicketRepository>(relaxed = true)

    private val handler = MessageEventHandler(
        attachmentStorageService = attachmentStorageService,
        channelIdentityRepository = channelIdentityRepository,
        channelRepository = channelRepository,
        conversationRegistry = conversationRegistry,
        conversationRepository = conversationRepository,
        eventBus = eventBus,
        ticketMessageAttachmentRepository = ticketMessageAttachmentRepository,
        ticketMessageRepository = ticketMessageRepository,
        ticketRepository = ticketRepository,
    )

    @Test
    fun `should_notStoreLocally_when_attachmentBytesEmpty`() = runTest {
        val attachment = mockk<Attachment>(relaxed = true) {
            every { attributes } returns emptyMap()
            every { contentSource } returns Buffer() // empty — VK Video case
            every { fileName } returns "video.mp4"
            every { kind } returns Attachment.Kind.VIDEO
            every { contentType } returns ContentType("video", "mp4")
        }

        handler.persistAttachmentMetadata(attachment, 1L, "vkontakte")

        verify(exactly = 0) { attachmentStorageService.store(any(), any(), any()) }
    }

    @Test
    fun `should_notStoreLocally_when_attachmentHasTelegramFileId`() = runTest {
        val attachment = mockk<Attachment>(relaxed = true) {
            every { attributes } returns mapOf("telegram.file_id" to JsonPrimitive("file-123"))
            every { contentType } returns ContentType.Image.JPEG
        }

        handler.persistAttachmentMetadata(attachment, 1L, "telegram")

        verify(exactly = 0) { attachmentStorageService.store(any(), any(), any()) }
    }

    @Test
    fun `should_notStoreLocally_when_attachmentHasVkUrl`() = runTest {
        val attachment = mockk<Attachment>(relaxed = true) {
            every { attributes } returns mapOf("vk.url" to JsonPrimitive("https://cdn.vk.com/photo.jpg"))
            every { contentType } returns ContentType.Image.JPEG
        }

        handler.persistAttachmentMetadata(attachment, 1L, "vkontakte")

        verify(exactly = 0) { attachmentStorageService.store(any(), any(), any()) }
    }

    @Test
    fun `should_storeLocally_when_attachmentHasNeitherFileIdNorVkUrl`() = runTest {
        val bytes = byteArrayOf(1, 2, 3, 4, 5)
        val attachment = mockk<Attachment>(relaxed = true) {
            every { attributes } returns emptyMap()
            every { contentSource } returns Buffer().also { it.write(bytes) }
            every { fileName } returns "report.pdf"
            every { kind } returns Attachment.Kind.DOCUMENT
            every { contentType } returns ContentType.Application.Pdf
            every { fileSize } returns bytes.size.toLong()
        }
        var capturedAttributes: JsonObject? = null
        coEvery {
            ticketMessageAttachmentRepository.create(
                messageId = any(), kind = any(), fileName = any(),
                contentType = any(), fileSize = any(), channelBrand = any(), attributes = any(),
            )
        } answers { capturedAttributes = arg(6); mockk(relaxed = true) }
        every { attachmentStorageService.store(any(), any(), any()) } returns "document/uuid.pdf"

        handler.persistAttachmentMetadata(attachment, 1L, "email")

        verify(exactly = 1) { attachmentStorageService.store(any(), "report.pdf", Attachment.Kind.DOCUMENT) }
        val storagePath = (capturedAttributes?.get("local.storage_path") as? JsonPrimitive)?.content
        assertEquals("document/uuid.pdf", storagePath)
    }

}
