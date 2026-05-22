package me.soknight.easydesk.core.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.soknight.easydesk.core.KeyedEnum

/**
 * [KSerializer] for enums implementing [KeyedEnum].
 *
 * Serializes by [KeyedEnum.key]; deserializes by case-insensitive key lookup.
 * Throws [IllegalArgumentException] if the decoded string does not match any entry.
 *
 * Prefer constructing instances via [KeyedEnum.serializer] rather than directly.
 *
 * @param E enum type implementing [KeyedEnum]
 * @param enumClass Java class of [E], used to enumerate constants at runtime
 * @see KeyedEnum.serializer
 */
class KeyedEnumSerializer<E>(private val enumClass: Class<E>) :
    KSerializer<E> where E : Enum<E>, E : KeyedEnum {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor(
        "me.soknight.easydesk.core.KeyedEnum<${enumClass.simpleName}>",
        PrimitiveKind.STRING,
    )

    private val allKeys by lazy {
        enumClass.enumConstants.joinToString(separator = ", ") { it.key }
    }

    override fun serialize(encoder: Encoder, value: E) =
        encoder.encodeString(value.key)

    override fun deserialize(decoder: Decoder): E =
        decoder.decodeString().let { key ->
            requireNotNull(findByKey(key)) {
                "key '$key' not present in '${enumClass.simpleName}', available: $allKeys"
            }
        }

    private fun findByKey(key: String?): E? {
        val key = key?.takeIf(String::isNotEmpty) ?: return null
        return enumClass.enumConstants.find { it.key.equals(key, ignoreCase = true) }
    }

}