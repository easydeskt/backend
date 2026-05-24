@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.api.response

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

@Serializable
data class TicketsCounters(
    val inProgress: Long,
    val open: Long,
    val resolved: Long,
)

@Serializable
data class WorkspaceMetrics(
    val avgResponseTime: Double,
    val ticketsCounters: TicketsCounters,
)

@Serializable
data class WorkspaceResponse(
    val metrics: WorkspaceMetrics,
    val name: String,
    val startedAt: Long,
    val superadminId: Uuid?,
    val versions: WorkspaceVersions,
)

@Serializable
data class WorkspaceVersions(
    val backend: String,
    val miniApp: String,
)
