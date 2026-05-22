@file:OptIn(ExperimentalUuidApi::class)

package me.soknight.easydesk.supervisor.api.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import me.soknight.easydesk.core.KeyedEnum
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Read-only view of an agent for supervisor surfaces.
 */
interface Agent {

    /** Internal UUID; stable public reference across all surfaces. */
    val identifier: Uuid

    /** Human-readable name shown in the UI and audit log. */
    val displayName: String

    /** `false` means the agent is soft-deleted and cannot act. */
    val isActive: Boolean

    /** Access level; [Role.ADMIN] is a strict superset of [Role.OPERATOR]. */
    val role: Role

    /** The role of an agent within the helpdesk system. */
    @Serializable(with = Role.Serializer::class)
    enum class Role(override val key: String) : KeyedEnum {

        ADMIN       ("admin"),
        OPERATOR    ("operator"),
        ;

        object Serializer : KSerializer<Role> by KeyedEnum.serializer()

    }

}
