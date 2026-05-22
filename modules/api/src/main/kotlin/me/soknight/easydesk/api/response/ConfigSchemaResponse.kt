package me.soknight.easydesk.api.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import me.soknight.easydesk.channel.api.config.ConfigField
import me.soknight.easydesk.channel.api.config.ConfigSchema
import me.soknight.easydesk.channel.api.config.ConfigSection

@Serializable
data class ConfigSchemaResponse(
    @SerialName("sections") val sections: List<ConfigSectionResponse>,
    @SerialName("order") val order: List<String>,
)

@Serializable
data class ConfigSectionResponse(
    @SerialName("key") val key: String,
    @SerialName("fields") val fields: List<JsonObject>,
    @SerialName("order") val order: List<String>,
)

fun ConfigSchema.toResponse() =
    ConfigSchemaResponse(sections.map { it.toSectionResponse() }, order)

fun ConfigSection.toSectionResponse() =
    ConfigSectionResponse(key, map { it.toFieldResponse() }, order)

fun ConfigField<*>.toFieldResponse(): JsonObject =
    buildJsonObject {
        put("key", key)
        put("type", typeKey())

        if (this@toFieldResponse is ConfigField.Selectable<*>) {
            put("variants", buildJsonArray {
                variants.forEach {
                    add(JsonPrimitive(it.toString()))
                }
            })
        }

        put("default_value", defaultValue?.toString())
        put("placeholder", placeholder)
        put("required", isRequired)
    }

private fun ConfigField<*>.typeKey(): String = when (this) {
    is ConfigField.Boolean -> "boolean"
    is ConfigField.Number.Byte -> "number.byte"
    is ConfigField.Number.Double -> "number.double"
    is ConfigField.Number.Float -> "number.float"
    is ConfigField.Number.Int -> "number.int"
    is ConfigField.Number.Long -> "number.long"
    is ConfigField.Number.Short -> "number.short"
    is ConfigField.Selectable.Enum<*> -> "selectable.enum"
    is ConfigField.Selectable.Literal -> "selectable.literal"
    is ConfigField.Text.Default -> "text.default"
    is ConfigField.Text.Password -> "text.password"
    is ConfigField.Text.Url -> "text.url"
}
