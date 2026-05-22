package me.soknight.easydesk.supervisor.api.model

import io.ktor.http.*

/**
 * Read-only view of a reply template for supervisor surfaces.
 */
interface ReplyTemplate {

    /** Internal auto-generated identifier. */
    val identifier: Long

    /** Unique human-readable label shown in the template picker. */
    val humanName: String

    /** Ordered list of attached files; at most 10 entries. */
    val attachments: List<Attachment>

    /** Template body with optional placeholders, or `null` for attachment-only templates. */
    val content: String?

    interface Attachment {

        /** Id of the stored attachment record in `service:storage`. */
        val attachmentId: Long

        /** MIME type of the file. */
        val contentType: ContentType

        /** Original filename from the platform. */
        val fileName: String

        /** Byte count, or `null` if not reported by the platform. */
        val fileSize: Long?

        /** Media type category (e.g. `"PHOTO"`, `"DOCUMENT"`). */
        val kind: String

        /** Id of the owning template. */
        val templateId: Long

    }

}
