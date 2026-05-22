package me.soknight.easydesk.channel.api.model

import io.ktor.http.*
import kotlinx.io.Source
import me.soknight.easydesk.channel.api.state.AttributesHolder
import me.soknight.easydesk.channel.api.state.ChannelScoped
import kotlin.time.Duration

/**
 * A file attachment within a [Message].
 *
 * All attachments share a common set of properties ([fileName], [contentType],
 * [fileSize], [contentSource]). Concrete subtypes carry additional metadata
 * specific to the attachment kind:
 * - [Audio] — audio files with duration, title, and performer
 * - [Document] — generic files without extra metadata
 * - [Photo] — images with width and height
 * - [Video] — video files with duration, width, and height
 * - [Voice] — voice messages with duration
 *
 * Note: [contentSource] is a one-shot [Source]. If the content needs to be
 * read more than once, the consumer is responsible for buffering.
 *
 * @see Message
 */
sealed interface Attachment : AttributesHolder, ChannelScoped {

    /** A [Source] that provides the raw file bytes. Read-once. */
    val contentSource: Source

    /** MIME type of the file (e.g., `image/png`, `application/pdf`). */
    val contentType: ContentType

    /** Original file name (e.g., `"screenshot.png"`, `"report.pdf"`). */
    val fileName: String

    /** File size in bytes, or `null` if unknown (some platforms report size as optional). */
    val fileSize: Long?

    /**
     * An audio file attachment (e.g., MP3, OGG).
     *
     * @property duration playback duration
     * @property performer artist or performer name, if available
     * @property title track title, if available
     */
    interface Audio : Attachment {

        val duration: Duration

        val performer: String?

        val title: String?

    }

    /** A generic file attachment without additional metadata. */
    interface Document : Attachment

    /**
     * An image attachment.
     *
     * @property height image height in pixels
     * @property width image width in pixels
     */
    interface Photo : Attachment {

        val height: Int

        val width: Int

    }

    /**
     * A video attachment.
     *
     * @property duration playback duration
     * @property height video height in pixels
     * @property width video width in pixels
     */
    interface Video : Attachment {

        val duration: Duration

        val height: Int

        val width: Int

    }

    /**
     * A voice message attachment.
     *
     * @property duration playback duration
     */
    interface Voice : Attachment {

        val duration: Duration

    }

}
