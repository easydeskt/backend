package me.soknight.easydesk.core

import kotlinx.serialization.KSerializer
import me.soknight.easydesk.core.serialization.KeyedEnumSerializer
import kotlin.enums.enumEntries

/**
 * Contract for enums that expose a stable string key for serialization and lookup.
 *
 * Implement this interface on enums that should be serialized by a human-readable key
 * rather than by name or ordinal. All lookups are case-insensitive.
 *
 * ```kotlin
 * @Serializable(with = Status.Serializer::class)
 * enum class Status(override val key: String) : KeyedEnum {
 *
 *     OPEN        ("open"),
 *     RESOLVED    ("resolved");
 *
 *     object Serializer : KSerializer<Status> by KeyedEnum.serializer()
 *
 * }
 * ```
 *
 * @see KeyedEnumSerializer
 */
interface KeyedEnum {

    /** Stable string identifier used for serialization and case-insensitive lookup. */
    val key: String

    companion object {

        /**
         * Returns `true` if any entry of [E] has a [key][KeyedEnum.key] matching [key]
         * (case-insensitive). Returns `false` for a `null` or blank [key].
         */
        inline fun <reified E> existsByKey(key: String?): Boolean where E : Enum<E>, E : KeyedEnum {
            val key = key?.takeIf(String::isNotEmpty) ?: return false
            return enumEntries<E>().any { it.key.equals(key, ignoreCase = true) }
        }

        /**
         * Returns the entry of [E] whose [key][KeyedEnum.key] matches [key] (case-insensitive),
         * or `null` if no match is found or [key] is `null`/blank.
         */
        inline fun <reified E> findByKey(key: String?): E? where E : Enum<E>, E : KeyedEnum {
            val key = key?.takeIf(String::isNotEmpty) ?: return null
            return enumEntries<E>().find { it.key.equals(key, ignoreCase = true) }
        }

        /**
         * Returns all [key][KeyedEnum.key] values of [E] joined by [separator].
         *
         * Useful for building error messages that list valid values.
         */
        inline fun <reified E> joinKeys(separator: String = ", "): String where E : Enum<E>, E : KeyedEnum =
            enumEntries<E>().joinToString(separator = separator) { it.key }

        /**
         * Creates a [KSerializer] for a [KeyedEnum] implementation.
         *
         * Serializes by [key][KeyedEnum.key]; deserializes by case-insensitive key lookup.
         *
         * Usage:
         * ```kotlin
         * @Serializable(with = Role.Serializer::class)
         * enum class Role(override val key: String) : KeyedEnum {
         *
         *     ADMIN       ("admin"),
         *     OPERATOR    ("operator");
         *
         *     object Serializer : KSerializer<Role> by KeyedEnum.serializer()
         *
         * }
         * ```
         */
        inline fun <reified E> serializer(): KSerializer<E> where E : Enum<E>, E : KeyedEnum =
            KeyedEnumSerializer(E::class.java)

    }

}
