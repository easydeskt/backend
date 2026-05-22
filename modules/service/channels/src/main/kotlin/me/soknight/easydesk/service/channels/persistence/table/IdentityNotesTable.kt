@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.channels.persistence.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

internal object IdentityNotesTable : LongIdTable("identity_notes") {

    val authorAgentId = uuid("author_agent_id")
    val createdAt = timestamp("created_at")
    val identityId = long("identity_id")
    val text = text("text")
    val updatedAt = timestamp("updated_at")

}
