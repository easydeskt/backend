package me.soknight.easydesk.channel.api.config

internal data class DefaultSection(
    override val key: String,
    override val order: List<String>,
    private val fields: List<ConfigField.Any>,
) : ConfigSection {

    override fun get(key: String) =
        fields.firstOrNull { it.key == key }

    override fun iterator() =
        fields.iterator()

    companion object : ConfigSection.Factory {

        override fun create(key: String, fields: List<ConfigField.Any>, order: List<String>) =
            DefaultSection(key, order, fields)

    }

}
