package me.soknight.easydesk.channel.email

import io.mockk.every
import io.mockk.mockk
import jakarta.mail.BodyPart
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.internet.MimeMessage
import me.soknight.easydesk.channel.email.internal.MimeMessageMapper
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class MimeMessageMapperTest {

    @Test
    fun `should_skipAttachment_when_partSizeExceedsLimit`() {
        val channel = mockk<EmailChannel>(relaxed = true)
        val mapper = MimeMessageMapper(channel)

        val bigPart = mockk<BodyPart>(relaxed = true) {
            every { isMimeType("text/plain") } returns false
            every { isMimeType("text/html") } returns false
            every { isMimeType("multipart/*") } returns false
            every { disposition } returns Part.ATTACHMENT
            every { fileName } returns "huge.bin"
            every { size } returns (51 * 1024 * 1024)   // 51 MB
            every { contentType } returns "application/octet-stream"
        }

        val multipart = mockk<Multipart>(relaxed = true) {
            every { count } returns 1
            every { getBodyPart(0) } returns bigPart
        }

        val jakartaMsg = mockk<MimeMessage>(relaxed = true) {
            every { from } returns arrayOf(jakarta.mail.internet.InternetAddress("user@example.com"))
            every { getHeader("Message-ID") } returns arrayOf("<id@example.com>")
            every { subject } returns "Test"
            every { getHeader("In-Reply-To") } returns null
            every { getHeader("References") } returns null
            every { sentDate } returns java.util.Date()
            every { isMimeType("text/plain") } returns false
            every { isMimeType("text/html") } returns false
            every { isMimeType("multipart/*") } returns true
            every { content } returns multipart
        }

        val result = mapper.map(jakartaMsg)

        assertTrue(result.message.attachments.isEmpty(), "Oversized attachment should be skipped")
    }

}
