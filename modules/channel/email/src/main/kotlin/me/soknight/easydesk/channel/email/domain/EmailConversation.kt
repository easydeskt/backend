package me.soknight.easydesk.channel.email.domain

import jakarta.mail.Authenticator
import jakarta.mail.PasswordAuthentication
import jakarta.mail.Session
import jakarta.mail.Transport
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeBodyPart
import jakarta.mail.internet.MimeMessage
import jakarta.mail.internet.MimeMultipart
import java.util.Properties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import me.soknight.easydesk.channel.api.ChannelActor
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.dsl.MessageBuilder
import me.soknight.easydesk.channel.api.model.Conversation
import me.soknight.easydesk.channel.api.model.Message
import me.soknight.easydesk.channel.email.EmailChannel
import me.soknight.easydesk.channel.email.config.EmailConfig
import me.soknight.easydesk.channel.email.dsl.EmailMessageBuilder

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
        send(replyToNativeId) { copyFrom(message) }

    override suspend fun send(replyToNativeId: String?, block: MessageBuilder.() -> Unit): Message {
        val builder = EmailMessageBuilder().apply(block)
        val config = channel.config
        val plainText = builder.plainText ?: ""

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
                setContent(buildMultipart(plainText))
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
            attributes = builder.builtAttributes,
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

    private fun buildMultipart(plainText: String): MimeMultipart {
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
