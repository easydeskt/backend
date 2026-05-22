@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.channels.data.repository

import me.soknight.easydesk.service.channels.data.domain.IdentityNote
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Persistence contract for [IdentityNote] management.
 *
 * All methods run inside a suspended transaction.
 */
interface IdentityNoteRepository {

    /**
     * Creates and persists a new identity note.
     *
     * @param identityId id of the identity to attach the note to
     * @param text note body
     * @param authorAgentId id of the writing agent
     * @return the persisted [IdentityNote] with its generated [id][IdentityNote.identifier]
     */
    suspend fun create(identityId: Long, text: String, authorAgentId: Uuid): IdentityNote

    /**
     * Returns all notes for the given identity in descending chronological order.
     *
     * @param identityId id of the target identity
     */
    suspend fun findByIdentity(identityId: Long): List<IdentityNote>

    /**
     * Returns the note with the given [id], or `null` if not found.
     */
    suspend fun findById(id: Long): IdentityNote?

    /**
     * Updates the text of an existing note.
     * Returns `null` if no note with [id] exists.
     *
     * @param id id of the note to update
     * @param text new note body
     */
    suspend fun update(id: Long, text: String): IdentityNote?

    /**
     * Deletes the note with the given [id].
     * Returns `true` if it existed and was deleted, `false` otherwise.
     */
    suspend fun delete(id: Long): Boolean

}
