package me.soknight.easydesk.channel.email.internal

import io.ktor.http.ContentType
import jakarta.mail.BodyPart
import jakarta.mail.Multipart
import jakarta.mail.Part
import jakarta.mail.UIDFolder
import jakarta.mail.internet.InternetAddress
import kotlin.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.MutableAttributes
import me.soknight.easydesk.channel.api.dsl.set
import me.soknight.easydesk.channel.email.EmailChannel
import me.soknight.easydesk.channel.email.EmailIdentity
import me.soknight.easydesk.channel.email.domain.EmailAttachment
import me.soknight.easydesk.channel.email.domain.EmailConversation
import me.soknight.easydesk.channel.email.domain.EmailMessage

internal class MimeMessageMapper(private val channel: EmailChannel) {

    data class MappedEmail(
        val conversation: EmailConversation,
        val message: EmailMessage,
        val timestamp: Instant,
    )

    fun map(jakartaMessage: jakarta.mail.Message): MappedEmail {
        val from = (jakartaMessage.from?.firstOrNull() as? InternetAddress)
            ?: error("Email has no From address")

        val messageId = jakartaMessage.getHeader("Message-ID")?.firstOrNull()
            ?: fallbackNativeId(jakartaMessage)

        val subject = jakartaMessage.subject ?: ""
        val inReplyTo = jakartaMessage.getHeader("In-Reply-To")?.firstOrNull()
        val references = jakartaMessage.getHeader("References")?.firstOrNull()

        val timestamp = jakartaMessage.sentDate?.let { Instant.fromEpochMilliseconds(it.time) }
            ?: jakartaMessage.receivedDate?.let { Instant.fromEpochMilliseconds(it.time) }
            ?: Instant.fromEpochMilliseconds(System.currentTimeMillis())

        val collectedAttachments = mutableListOf<EmailAttachment>()
        var plainText: String? = null
        walkPart(jakartaMessage, collectedAttachments) { text -> if (plainText == null) plainText = text }

        val msgAttributes: MutableAttributes = mutableMapOf()
        msgAttributes["email.message_id"] = messageId
        msgAttributes["email.subject"] = subject
        inReplyTo?.let { msgAttributes["email.in_reply_to"] = it }
        references?.let { msgAttributes["email.references"] = it }
        from.personal?.let { msgAttributes["email.from_name"] = it }

        val convAttributes = mapOf("email.subject" to JsonPrimitive(subject))

        val conversation = EmailConversation(
            attributes = convAttributes,
            channel = channel,
            recipientAddress = from.address,
        )

        val message = EmailMessage(
            conversation = conversation,
            nativeId = messageId,
            sender = EmailIdentity(from.address),
            receiver = ChannelActor.System,
            plainText = plainText,
            attachments = collectedAttachments,
            attributes = msgAttributes,
        )

        return MappedEmail(conversation, message, timestamp)
    }

    private fun fallbackNativeId(msg: jakarta.mail.Message): String {
        val uid = (msg.folder as? UIDFolder)?.getUID(msg)
        return if (uid != null) "uid:$uid" else "num:${msg.messageNumber}"
    }

    private fun walkPart(
        part: Part,
        attachments: MutableList<EmailAttachment>,
        onPlainText: (String) -> Unit,
    ) {
        when {
            part.isMimeType("text/plain") ->
                onPlainText(part.content as? String ?: "")
            part.isMimeType("text/html") ->
                onPlainText(stripHtml(part.content as? String ?: ""))
            part.isMimeType("multipart/*") -> {
                val multipart = part.content as Multipart
                for (i in 0 until multipart.count) walkPart(multipart.getBodyPart(i), attachments, onPlainText)
            }
            part is BodyPart && isAttachment(part) -> {
                val rawType = part.contentType.substringBefore(";").trim()
                attachments.add(
                    EmailAttachment(
                        channel = channel,
                        attributes = emptyMap(),
                        contentType = runCatching { ContentType.parse(rawType) }
                            .getOrDefault(ContentType.Application.OctetStream),
                        fileName = part.fileName ?: "attachment",
                        fileSize = part.size.takeIf { it >= 0 }?.toLong(),
                        bytes = part.inputStream.use { it.readBytes() },
                    )
                )
            }
        }
    }

    private fun isAttachment(part: BodyPart): Boolean =
        Part.ATTACHMENT.equals(part.disposition, ignoreCase = true) ||
                (!part.isMimeType("text/*") && part.fileName != null)

    private fun stripHtml(html: String): String =
        html
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&amp;", "&")
            .replace("&apos;", "'")
            .replace("&gt;", ">")
            .replace("&lt;", "<")
            .replace("&nbsp;", " ")
            .replace("&quot;", "\"")
            .trim()

}
