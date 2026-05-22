@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.api.binding

import me.soknight.easydesk.supervisor.api.SupervisorBrand
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Binds an agent to a supervisor surface.
 *
 * The [nativeId] is surface-specific (e.g., Telegram `user_id`). A single
 * agent may have at most one binding per [brand].
 *
 * @see SupervisorBrand
 */
data class AgentSupervisorBinding(
    /** Unique identifier of the agent. */
    val agentId: Uuid,
    /** The supervisor surface type. */
    val brand: SupervisorBrand,
    /** Platform-native identifier (e.g., Telegram `user_id`). */
    val nativeId: String,
    /** When the binding was created. */
    val createdAt: Instant = Clock.System.now(),
)
