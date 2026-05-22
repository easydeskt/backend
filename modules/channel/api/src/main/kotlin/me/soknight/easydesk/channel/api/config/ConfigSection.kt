package me.soknight.easydesk.channel.api.config

/**
 * A named group of [ConfigField]s within a [ConfigSchema].
 *
 * Iterating a section yields its fields in declaration order.
 * Sections with an empty [key] (`""`) represent root-level (ungrouped) fields.
 *
 * @see ConfigField
 * @see ConfigSchema
 */
sealed interface ConfigSection : Iterable<ConfigField.Any> {

    /** Identifies this section within its schema. Empty string for the root section. */
    val key: String

    /**
     * Preferred display order for fields within this section, expressed as a list of field keys.
     * Fields not present in the list are shown after the ordered ones, in their declaration order.
     * An empty list means no explicit ordering — declaration order is used for all fields.
     */
    val order: List<String>

    /** Returns the field with the given [key], or `null` if no such field exists. */
    operator fun get(key: String): ConfigField.Any?

    /**
     * Creates [ConfigSection] instances.
     *
     * The default implementation is pre-registered in [ConfigSchemaFactories.section].
     */
    interface Factory {

        fun create(
            key: String,
            fields: List<ConfigField.Any>,
            order: List<String> = emptyList(),
        ): ConfigSection

    }

}
