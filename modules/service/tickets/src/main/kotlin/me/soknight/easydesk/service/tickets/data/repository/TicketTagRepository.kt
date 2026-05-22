@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.tickets.data.repository

import me.soknight.easydesk.service.tickets.data.domain.TicketTag
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Persistence contract for [TicketTag] management.
 *
 * All methods run inside a suspended transaction.
 */
interface TicketTagRepository {

    /**
     * Attaches a tag to a ticket.
     *
     * The pair `(ticketId, tagId)` must be unique; a duplicate insert will throw
     * a database constraint violation.
     *
     * @param ticketId id of the ticket
     * @param tagId id of the tag to attach
     * @param addedByAgentId id of the agent performing the action
     */
    suspend fun addToTicket(ticketId: Long, tagId: Long, addedByAgentId: Uuid)

    /**
     * Creates and persists a new tag.
     *
     * [name] must be unique system-wide; a duplicate insert will throw a database
     * constraint violation.
     *
     * @param name unique label (max 32 characters)
     * @param color optional RGBA int32 color for UI display
     * @return the persisted [TicketTag] with its generated [id][TicketTag.identifier]
     */
    suspend fun create(name: String, color: Int? = null): TicketTag

    /**
     * Deletes the tag with the given [id] and all its ticket associations.
     * Returns `true` if the tag existed and was deleted, `false` otherwise.
     */
    suspend fun delete(id: Long): Boolean

    /**
     * Returns all tags.
     */
    suspend fun findAll(): List<TicketTag>

    /**
     * Returns the tag with the given [id], or `null` if not found.
     */
    suspend fun findById(id: Long): TicketTag?

    /**
     * Returns the tag with the given [name], or `null` if not found.
     */
    suspend fun findByName(name: String): TicketTag?

    /**
     * Returns all tags attached to the given ticket, ordered by their `sort_order`.
     */
    suspend fun findByTicket(ticketId: Long): List<TicketTag>

    /**
     * Returns ids of all tickets that have the given tag attached.
     *
     * @param tagId id of the tag to look up
     */
    suspend fun findTicketIdsByTag(tagId: Long): List<Long>

    /**
     * Detaches a tag from a ticket.
     * Returns `true` if the tag was attached and has been removed, `false` otherwise.
     *
     * @param ticketId id of the ticket
     * @param tagId id of the tag to detach
     */
    suspend fun removeFromTicket(ticketId: Long, tagId: Long): Boolean

    /**
     * Bulk-replaces all tag assignments for the ticket with the given ordered list.
     * All existing assignments are removed and re-inserted with `sort_order` matching the list position.
     *
     * @param ticketId id of the ticket to update
     * @param orderedTagIds tag ids in the desired display order
     * @param byAgentId id of the agent performing the action
     */
    suspend fun setTags(ticketId: Long, orderedTagIds: List<Long>, byAgentId: Uuid)

    /**
     * Updates a tag's name and color.
     * Returns the updated [TicketTag], or `null` if not found.
     *
     * @param id id of the tag to update
     * @param name new unique label (max 32 characters)
     * @param color new RGBA int32 color, or `null` to clear
     */
    suspend fun update(id: Long, name: String, color: Int? = null): TicketTag?

}
