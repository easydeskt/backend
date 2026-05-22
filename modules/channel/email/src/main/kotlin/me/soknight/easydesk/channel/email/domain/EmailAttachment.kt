package me.soknight.easydesk.channel.email.domain

import io.ktor.http.ContentType
import kotlinx.io.Buffer
import kotlinx.io.Source
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.model.Attachment

/**
 * Email file attachment. Bytes are read eagerly from the MIME part at construction time
 * so the content remains available after the IMAP folder is closed.
 */
class EmailAttachment(
    override val channel: Channel,
    override val attributes: Attributes,
    override val contentType: ContentType,
    override val fileName: String,
    override val fileSize: Long?,
    private val bytes: ByteArray,
) : Attachment.Document {

    override val contentSource: Source
        get() = Buffer().apply { write(bytes) }

}
