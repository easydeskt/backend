@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.response

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceMetrics(
    @SerialName("avg_response_time") val avgResponseTime: Double,
    @SerialName("open_tickets") val openTickets: Long,
    @SerialName("tickets_in_progress") val ticketsInProgress: Long,
    @SerialName("tickets_resolved") val ticketsResolved: Long,
)

@Serializable
data class WorkspaceResponse(
    val metrics: WorkspaceMetrics,
    val name: String,
    @SerialName("superadmin_id") val superadminId: Uuid?,
)
