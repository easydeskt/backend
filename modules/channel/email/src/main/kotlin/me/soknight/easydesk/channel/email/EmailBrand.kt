package me.soknight.easydesk.channel.email

import me.soknight.easydesk.channel.api.ChannelBrand
import me.soknight.easydesk.channel.api.ChannelBrand.Feature

/**
 * [ChannelBrand] for Email (IMAP/SMTP).
 *
 * Supports rich text formatting (HTML with plain-text fallback).
 * Does not support message editing or deletion — once an email
 * is sent via SMTP, it is immutable in the recipient's mailbox.
 */
object EmailBrand : ChannelBrand {

    override val identifier get() = "mail"

    override val humanName get() = "Email"

    override val supportedFeatures: List<Feature>
        get() = listOf(Feature.MESSAGE_FORMAT)

}
