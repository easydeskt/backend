package me.soknight.easydesk.service.storage.data.domain

import io.ktor.http.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant
import kotlinx.io.Source
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.api.dsl.Attributes
import me.soknight.easydesk.channel.api.model.Attachment as ChannelAttachment
import me.soknight.easydesk.channel.api.model.Attachment.Kind
import me.soknight.easydesk.service.storage.data.service.AttachmentStorageService

/**
 * A binary file managed by `service:storage`.
 *
 * [storagePath] is an opaque string; consumers must not parse or construct it directly —
 * only `service:storage` knows how to resolve it to actual bytes.
 *
 * Media-specific metadata (duration, dimensions, performer, title) is stored in [attributes]
 * as JSON and exposed via type-safe getters on the typed subclasses.
 *
 * Typed subclasses ([Audio], [Document], [Photo], [Sticker], [Video], [Voice]) implement the
 * corresponding [ChannelAttachment] sub-interfaces, allowing them to be passed into the
 * `channel:api` pipeline. The base class does not directly implement [ChannelAttachment] because
 * it is a `sealed interface` whose direct implementations are restricted to the `channel:api` module.
 *
 * @param identifier internal auto-generated identifier
 * @param kind media type category
 * @param fileName original filename from the platform
 * @param contentType MIME type
 * @param fileSize byte count, or `null` if not reported by the platform
 * @param storagePath opaque path used by `service:storage` to retrieve the file
 * @param attributes extensible platform-specific metadata; holds media properties for typed subclasses
 * @param createdAt timestamp of recording
 * @param channel the channel this attachment belongs to
 * @param storageService service used to open raw file bytes on demand
 */
sealed class Attachment(
    val identifier: Long,
    open val kind: Kind,
    val fileName: String,
    val contentType: ContentType,
    val fileSize: Long?,
    val storagePath: String,
    val attributes: Attributes,
    val createdAt: Instant,
    val channel: Channel,
    private val storageService: AttachmentStorageService,
) {

    protected constructor(base: Attachment, kind: Kind) : this(
        base.identifier, kind, base.fileName, base.contentType, base.fileSize,
        base.storagePath, base.attributes, base.createdAt, base.channel, base.storageService,
    )

    val contentSource: Source
        get() = storageService.openSource(storagePath)

    /** Concrete base for direct instantiation within this module. */
    internal class Base(
        identifier: Long,
        kind: Kind,
        fileName: String,
        contentType: ContentType,
        fileSize: Long?,
        storagePath: String,
        attributes: Attributes,
        createdAt: Instant,
        channel: Channel,
        storageService: AttachmentStorageService,
    ) : Attachment(
        identifier, kind, fileName, contentType, fileSize,
        storagePath, attributes, createdAt, channel, storageService,
    )

    /**
     * An audio file (e.g., MP3, OGG). Reads `duration_ms`, `performer`, `title` from [attributes].
     *
     * Implements [ChannelAttachment.Audio]; inherits [contentSource], [fileName], [contentType],
     * [fileSize], [channel], and [attributes] from the base class.
     */
    class Audio(base: Attachment) : Attachment(base, Kind.AUDIO), ChannelAttachment.Audio {

        override val kind: Kind get() = Kind.AUDIO

        override val duration: Duration
            get() = (attributes["duration_ms"]?.jsonPrimitive?.long
                ?: error("duration_ms missing in attributes for Audio id=$identifier")).milliseconds

        override val performer: String?
            get() = attributes["performer"]?.jsonPrimitive?.contentOrNull

        override val title: String?
            get() = attributes["title"]?.jsonPrimitive?.contentOrNull

    }

    /**
     * A generic file attachment without additional metadata.
     */
    class Document(base: Attachment) : Attachment(base, Kind.DOCUMENT), ChannelAttachment.Document {

        override val kind: Kind get() = Kind.DOCUMENT

    }

    /** An image attachment. Reads `height` and `width` from [attributes]. */
    class Photo(base: Attachment) : Attachment(base, Kind.PHOTO), ChannelAttachment.Photo {

        override val kind: Kind get() = Kind.PHOTO

        override val height: Int
            get() = attributes["height"]?.jsonPrimitive?.int
                ?: error("height missing in attributes for Photo id=$identifier")

        override val width: Int
            get() = attributes["width"]?.jsonPrimitive?.int
                ?: error("width missing in attributes for Photo id=$identifier")

    }

    /** A sticker attachment. Reads `height` and `width` from [attributes]. */
    class Sticker(base: Attachment) : Attachment(base, Kind.STICKER), ChannelAttachment.Sticker {

        override val kind: Kind get() = Kind.STICKER

        override val height: Int
            get() = attributes["height"]?.jsonPrimitive?.int
                ?: error("height missing in attributes for Sticker id=$identifier")

        override val width: Int
            get() = attributes["width"]?.jsonPrimitive?.int
                ?: error("width missing in attributes for Sticker id=$identifier")

    }

    /** A video attachment. Reads `duration_ms`, `height`, `width` from [attributes]. */
    class Video(base: Attachment) : Attachment(base, Kind.VIDEO), ChannelAttachment.Video {

        override val kind: Kind get() = Kind.VIDEO

        override val duration: Duration
            get() = (attributes["duration_ms"]?.jsonPrimitive?.long
                ?: error("duration_ms missing in attributes for Video id=$identifier")).milliseconds

        override val height: Int
            get() = attributes["height"]?.jsonPrimitive?.int
                ?: error("height missing in attributes for Video id=$identifier")

        override val width: Int
            get() = attributes["width"]?.jsonPrimitive?.int
                ?: error("width missing in attributes for Video id=$identifier")

    }

    /** A voice message. Reads `duration_ms` from [attributes]. */
    class Voice(base: Attachment) : Attachment(base, Kind.VOICE), ChannelAttachment.Voice {

        override val kind: Kind get() = Kind.VOICE

        override val duration: Duration
            get() = (attributes["duration_ms"]?.jsonPrimitive?.long
                ?: error("duration_ms missing in attributes for Voice id=$identifier")).milliseconds

    }

}
