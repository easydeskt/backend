package me.soknight.easydesk.supervisor.api.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import me.soknight.easydesk.core.KeyedEnum

/**
 * Discriminates who sent a [TicketMessage].
 */
@Serializable(with = ActorKind.Serializer::class)
enum class ActorKind(override val key: String) : KeyedEnum {

    AGENT       ("agent"),
    IDENTITY    ("identity"),
    SYSTEM      ("system"),
    ;

    object Serializer : KSerializer<ActorKind> by KeyedEnum.serializer()

}
