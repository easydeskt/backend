@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.service.templates.persistence.table

import org.jetbrains.exposed.v1.core.dao.id.LongIdTable
import org.jetbrains.exposed.v1.datetime.timestamp
import kotlin.uuid.ExperimentalUuidApi

internal object ReplyTemplatesTable : LongIdTable("reply_templates") {

    val content = text("content").nullable()
    val createdAt = timestamp("created_at")
    val createdBy = uuid("created_by")
    val name = varchar("name", 128)
    val updatedAt = timestamp("updated_at")

}
