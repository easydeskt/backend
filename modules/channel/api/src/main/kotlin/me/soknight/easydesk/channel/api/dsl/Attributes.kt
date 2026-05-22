package me.soknight.easydesk.channel.api.dsl

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * Immutable view of platform-specific metadata as key-value pairs.
 *
 * Used by [AttributesHolder][me.soknight.easydesk.channel.api.state.AttributesHolder]
 * to expose opaque data on channels, messages, and attachments.
 *
 * @see MutableAttributes
 */
typealias Attributes = Map<String, JsonElement>

/**
 * Mutable view of platform-specific metadata.
 *
 * Used in DSL builders (e.g., [MessageBuilder]) to allow modification.
 * Convenience [set] operators for [Boolean], [Number], and [String]
 * automatically wrap values in [JsonPrimitive].
 *
 * ```kotlin
 * channel.send {
 *     attributes {
 *         this["priority"] = "high"
 *         this["silent"] = true
 *         this["retry_count"] = 3
 *     }
 * }
 * ```
 *
 * @see Attributes
 * @see MessageBuilder.attributes
 */
typealias MutableAttributes = MutableMap<String, JsonElement>

/** Sets the attribute [key] to a [Boolean] value wrapped in [JsonPrimitive]. */
operator fun MutableAttributes.set(key: String, value: Boolean?) =
    set(key, JsonPrimitive(value))

/** Sets the attribute [key] to a [Number] value wrapped in [JsonPrimitive]. */
operator fun MutableAttributes.set(key: String, value: Number?) =
    set(key, JsonPrimitive(value))

/** Sets the attribute [key] to a [String] value wrapped in [JsonPrimitive]. */
operator fun MutableAttributes.set(key: String, value: String?) =
    set(key, JsonPrimitive(value))
