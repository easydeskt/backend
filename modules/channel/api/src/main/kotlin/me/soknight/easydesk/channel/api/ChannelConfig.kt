package me.soknight.easydesk.channel.api

/**
 * Marker interface for platform-specific channel configuration.
 *
 * Each [ChannelProvider] implementation defines its own [ChannelConfig]
 * subtype with the settings required by the platform (e.g., bot token,
 * polling parameters, SMTP credentials).
 *
 * Configurations are typically deserialized from external TOML files.
 *
 * @see ChannelProvider.config
 */
interface ChannelConfig