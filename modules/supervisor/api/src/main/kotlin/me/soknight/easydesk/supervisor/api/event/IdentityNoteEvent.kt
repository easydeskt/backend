package me.soknight.easydesk.supervisor.api.event

import me.soknight.easydesk.core.event.Event
import me.soknight.easydesk.supervisor.api.model.IdentityNote

/**
 * Events that describe changes to identity notes.
 *
 * Identity notes are agent-written annotations on a
 * [ChannelIdentity][me.soknight.easydesk.supervisor.api.model.ChannelIdentity].
 * Published by the service layer; consumed by supervisor surfaces that display client context.
 *
 * @see me.soknight.easydesk.supervisor.api.model.IdentityNote
 */
sealed interface IdentityNoteEvent : Event {

    /** A note was added to an identity. */
    data class Added(val note: IdentityNote) : IdentityNoteEvent

    /** An existing note was edited. */
    data class Edited(val note: IdentityNote) : IdentityNoteEvent

    /** A note was removed. Carries minimal context since the full record is gone. */
    data class Removed(
        val identityId: Long,
        val noteId: Long,
    ) : IdentityNoteEvent

}
