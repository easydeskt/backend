package me.soknight.easydesk.channel.api.config

/**
 * The top-level configuration contract for a channel provider.
 *
 * A schema is itself a [ConfigSection] (the unnamed root section) and additionally
 * exposes a list of named [sections]. The admin UI renders each section as a
 * collapsible group of inputs.
 *
 * Build schemas with the [me.soknight.easydesk.channel.api.dsl.channelConfigSchema] DSL:
 *
 * ```kotlin
 * val SCHEMA by channelConfigSchema {
 *     password("token", required = true)
 *
 *     section("polling") {
 *         int("timeout_seconds", defaultValue = 30)
 *     }
 * }
 * ```
 *
 * @see ConfigSection
 * @see ConfigField
 * @see me.soknight.easydesk.channel.api.dsl.channelConfigSchema
 */
sealed interface ConfigSchema : ConfigSection {

    /** Named sections declared inside this schema, in declaration order. */
    val sections: List<ConfigSection>

    /**
     * Creates [ConfigSchema] instances.
     *
     * The default implementation is pre-registered in [ConfigSchemaFactories.schema].
     */
    interface Factory {

        fun create(
            sections: List<ConfigSection>,
            order: List<String> = emptyList(),
        ): ConfigSchema

    }

}
