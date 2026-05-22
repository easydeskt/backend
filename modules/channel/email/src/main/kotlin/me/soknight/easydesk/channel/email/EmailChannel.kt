package me.soknight.easydesk.channel.email

import me.soknight.easydesk.channel.api.Channel
import me.soknight.easydesk.channel.email.config.EmailConfig

class EmailChannel(
    override val identifier: String,
    override val humanName: String = identifier,
    override val config: EmailConfig,
) : Channel {

    override val provider get() = EmailProvider

    override fun toString(): String =
        "$humanName (${EmailBrand.humanName})"

}
