package me.soknight.easydesk.service.storage

import io.ktor.http.ContentType
import io.mockk.mockk
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlinx.serialization.json.JsonPrimitive
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.service.storage.data.domain.Attachment as DomainAttachment
import me.soknight.easydesk.service.storage.data.service.AttachmentStorageService
import org.junit.jupiter.api.Test

class AttachmentEntityTest {

    private val storageService = mockk<AttachmentStorageService>()

    @Test
    fun `should_mapToStickerSubclass_when_kindIsSticker`() {
        val base = DomainAttachment.Base(
            identifier = 1L,
            kind = Attachment.Kind.STICKER,
            fileName = "sticker.webp",
            contentType = ContentType.Image.Any,
            fileSize = 12_000L,
            storagePath = "sticker/uuid.webp",
            attributes = mapOf(
                "height" to JsonPrimitive(512),
                "width" to JsonPrimitive(512),
            ),
            createdAt = Clock.System.now(),
            channel = mockk<Channel>(),
            storageService = storageService,
        )

        val result = DomainAttachment.Sticker(base)

        assertIs<Attachment.Sticker>(result)
    }

}
