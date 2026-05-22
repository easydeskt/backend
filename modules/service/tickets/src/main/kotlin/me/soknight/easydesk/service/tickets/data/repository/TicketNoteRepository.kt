@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.data.repository

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.soknight.easydesk.service.tickets.data.domain.TicketNote

/**
 * Persistence contract for [TicketNote] management.
 *
 * All methods run inside a suspended transaction.
 */
interface TicketNoteRepository {

    /**
     * Creates and persists a new ticket note.
     *
     * @param ticketId id of the ticket to attach the note to
     * @param text note body
     * @param authorAgentId id of the writing agent
     * @return the persisted [TicketNote] with its generated [id][TicketNote.identifier]
     */
    suspend fun create(ticketId: Long, text: String, authorAgentId: Uuid): TicketNote

    /**
     * Deletes the note with the given [id].
     * Returns `true` if it existed and was deleted, `false` otherwise.
     */
    suspend fun delete(id: Long): Boolean

    /**
     * Returns all notes for the given ticket in descending chronological order.
     *
     * @param ticketId id of the target ticket
     */
    suspend fun findByTicket(ticketId: Long): List<TicketNote>

    /**
     * Returns the note with the given [id], or `null` if not found.
     */
    suspend fun findById(id: Long): TicketNote?

    /**
     * Updates the text of an existing note.
     * Returns `null` if no note with [id] exists.
     *
     * @param id id of the note to update
     * @param text new note body
     */
    suspend fun update(id: Long, text: String): TicketNote?

}
