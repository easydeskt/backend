@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.response

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TicketsCounters(
    @SerialName("in_progress") val inProgress: Long,
    val open: Long,
    val resolved: Long,
)

@Serializable
data class WorkspaceMetrics(
    @SerialName("avg_response_time") val avgResponseTime: Double,
    @SerialName("tickets_counters") val ticketsCounters: TicketsCounters,
)

@Serializable
data class WorkspaceResponse(
    val metrics: WorkspaceMetrics,
    val name: String,
    @SerialName("started_at") val startedAt: Long,
    @SerialName("superadmin_id") val superadminId: Uuid?,
    val versions: WorkspaceVersions,
)

@Serializable
data class WorkspaceVersions(
    val backend: String,
    val miniApp: String?,
)
