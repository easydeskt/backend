package me.soknight.easydesk.channel.api.config

internal data class DefaultSchema(
    override val sections: List<ConfigSection>,
    override val order: List<String>,
) : ConfigSchema {

    override val key = ""

    override fun get(key: String) =
        sections.asSequence()
            .flatMap { it.asSequence() }
            .firstOrNull { field -> field.key == key }

    override fun iterator() =
        sections.asSequence()
            .flatMap { it.asSequence() }
            .iterator()

    companion object : ConfigSchema.Factory {

        override fun create(sections: List<ConfigSection>, order: List<String>): ConfigSchema =
            DefaultSchema(sections = sections, order = order)

    }

}
