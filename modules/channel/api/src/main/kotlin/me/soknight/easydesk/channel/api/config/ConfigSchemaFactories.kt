package me.soknight.easydesk.channel.api.config

/**
 * Central registry that wires the [ConfigField.Factory], [ConfigSection.Factory],
 * and [ConfigSchema.Factory] implementations used by the
 * [me.soknight.easydesk.channel.api.dsl.channelConfigSchema] DSL.
 *
 * All three properties are pre-populated with the default internal implementations.
 * Replace any of them before schemas are first accessed to substitute a custom factory.
 */
object ConfigSchemaFactories {

    /** Factory used to create individual [ConfigField] instances. */
    var field: ConfigField.Factory = DefaultConfigFieldFactory

    /** Factory used to create [ConfigSchema] instances from a list of sections. */
    var schema: ConfigSchema.Factory = DefaultSchema

    /** Factory used to create [ConfigSection] instances from a key and field list. */
    var section: ConfigSection.Factory = DefaultSection

}
