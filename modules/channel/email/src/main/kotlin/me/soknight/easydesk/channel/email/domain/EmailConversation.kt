package me.soknight.easydesk.channel.email.domain

import jakarta.activation.DataHandler
import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import jakarta.mail.util.ByteArrayDataSource
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Attachment
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.email.EmailChannel
import me.soknight.easydesk.channel.email.config.EmailConfig
import me.soknight.easydesk.channel.email.dsl.EmailMessageBuilder
import me.soknight.easydesk.core.logging.getLogger
import me.soknight.easydesk.core.logging.warn

private val logger = getLogger(EmailConversation::class.java)

/**
 * Email-specific [Conversation] that sends replies via SMTP.
 *
 * @property recipientAddress the client's email address (From header of the received email)
 */
class EmailConversation(
    override val attributes: Attributes,
    override val channel: EmailChannel,
    val recipientAddress: String,
) : Conversation {

    override suspend fun send(message: Message, replyToNativeId: String?): Message =
        sendInternal(message.plainText ?: "", message.attachments, replyToNativeId)

    override suspend fun send(replyToNativeId: String?, block: MessageBuilder.() -> Unit): Message {
        val builder = EmailMessageBuilder().apply(block)
        return sendInternal(builder.plainText ?: "", builder.builtAttachments, replyToNativeId)
    }

    private suspend fun sendInternal(
        plainText: String,
        attachments: List<Attachment>,
        replyToNativeId: String?,
    ): Message {
        val config = channel.config

        val sentNativeId = withContext(Dispatchers.IO) {
            val session = createSmtpSession(config.smtp)
            val msg = MimeMessage(session).apply {
                setFrom(buildFromAddress(config))
                setRecipient(jakarta.mail.Message.RecipientType.TO, InternetAddress(recipientAddress))
                config.replyTo?.let { setReplyTo(arrayOf(InternetAddress(it))) }
                setSubject(buildSubject(), "UTF-8")
                if (replyToNativeId != null) {
                    setHeader("In-Reply-To", replyToNativeId)
                    setHeader("References", replyToNativeId)
                }
                setContent(buildContent(plainText, attachments))
                saveChanges()
            }
            Transport.send(msg, config.smtp.username, config.smtp.password)
            "<${msg.messageID ?: java.util.UUID.randomUUID()}>"
        }

        return EmailMessage(
            conversation = this,
            nativeId = sentNativeId,
            sender = ChannelActor.System,
            receiver = ChannelActor.Unknown,
            plainText = plainText.ifBlank { null },
            attachments = attachments.filter { it.kind != Attachment.Kind.STICKER },
        )
    }

    private fun buildSubject(): String {
        val raw = (attributes["email.subject"] as? JsonPrimitive)?.contentOrNull ?: return ""
        return if (raw.startsWith("Re: ", ignoreCase = true)) raw else "Re: $raw"
    }

    private fun buildFromAddress(config: EmailConfig): InternetAddress {
        val name = config.from.name
        return if (name != null) InternetAddress(config.from.address, name, "UTF-8")
        else InternetAddress(config.from.address)
    }

    internal fun buildContent(plainText: String, attachments: List<Attachment>): MimeMultipart {
        val alternative = buildMultipartAlternative(plainText)
        val nonStickerAttachments = attachments.filter { it.kind != Attachment.Kind.STICKER }
        if (nonStickerAttachments.isEmpty()) return alternative

        return MimeMultipart("mixed").apply {
            addBodyPart(MimeBodyPart().apply { setContent(alternative) })
            for (att in nonStickerAttachments) {
                runCatching {
                    addBodyPart(MimeBodyPart().apply {
                        dataHandler = DataHandler(
                            ByteArrayDataSource(att.contentSource.readByteArray(), att.contentType.toString())
                        )
                        fileName = att.fileName
                        disposition = jakarta.mail.Part.ATTACHMENT
                    })
                }.onFailure { logger.warn(it) { "Failed to attach '${att.fileName}' — skipping" } }
            }
        }
    }

    private fun buildMultipartAlternative(plainText: String): MimeMultipart {
        val html = "<html><body><p>${plainText.replace("\n", "<br>")}</p></body></html>"
        return MimeMultipart("alternative").apply {
            addBodyPart(MimeBodyPart().apply { setText(plainText, "UTF-8", "plain") })
            addBodyPart(MimeBodyPart().apply { setText(html, "UTF-8", "html") })
        }
    }

    private fun createSmtpSession(smtp: EmailConfig.Smtp): Session {
        val host = smtp.host ?: error("SMTP host is not configured for channel '${channel.humanName}'")
        val props = Properties().apply {
            put("mail.smtp.host", host)
            put("mail.smtp.port", smtp.port.toString())
            put("mail.smtp.auth", "true")
            put("mail.smtp.connectiontimeout", "10000")
            put("mail.smtp.timeout", "30000")
            if (smtp.shouldStartTLS) put("mail.smtp.starttls.enable", "true")
        }
        return Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication() =
                PasswordAuthentication(smtp.username, smtp.password)
        })
    }

}
