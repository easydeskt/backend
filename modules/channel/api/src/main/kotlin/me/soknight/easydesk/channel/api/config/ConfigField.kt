package me.soknight.easydesk.channel.api.config

import kotlin.Any as KotlinAny
import kotlin.Boolean as KotlinBoolean
import kotlin.Byte as KotlinByte
import kotlin.Double as KotlinDouble
import kotlin.Enum as KotlinEnum
import kotlin.Float as KotlinFloat
import kotlin.Int as KotlinInt
import kotlin.Long as KotlinLong
import kotlin.Number as KotlinNumber
import kotlin.Short as KotlinShort

/**
 * A single typed configuration field belonging to a [ConfigSection].
 *
 * The field type is encoded by the sealed subtype hierarchy — there is no separate
 * discriminator enum. Consumers pattern-match with `when (field)` to determine the type:
 *
 * ```kotlin
 * when (field) {
 *     is ConfigField.Text.Password -> // render password input
 *     is ConfigField.Text.Url      -> // render URL input
 *     is ConfigField.Text          -> // render plain text input
 *     is ConfigField.Number.Int    -> // render integer input
 *     // ...
 * }
 * ```
 *
 * @param T the Kotlin type of the field value
 * @see ConfigSection
 * @see ConfigSchema
 */
sealed interface ConfigField<T> {

    /** Convenience alias for a field with an unknown value type. */
    typealias Any = ConfigField<*>

    /** Unique key used to identify this field within its section. */
    val key: String

    /** Pre-filled value shown when the field has not been set by the user. */
    val defaultValue: T?
        get() = null

    /** Whether the field must be filled before the configuration can be saved. */
    val isRequired: KotlinBoolean
        get() = false

    /** Hint text displayed inside the input when the field is empty. */
    val placeholder: String?
        get() = null

    interface Boolean : ConfigField<KotlinBoolean> {
        override val defaultValue: KotlinBoolean get() = false
    }

    /** Numeric fields. Covers all standard Kotlin number primitives. */
    sealed interface Number<T : KotlinNumber> : ConfigField<T> {

        interface Byte : Number<KotlinByte> {
            override val defaultValue: KotlinByte get() = 0
        }

        interface Double : Number<KotlinDouble> {
            override val defaultValue: KotlinDouble get() = 0.0
        }

        interface Float : Number<KotlinFloat> {
            override val defaultValue: KotlinFloat get() = 0.0F
        }

        interface Int : Number<KotlinInt> {
            override val defaultValue: KotlinInt get() = 0
        }

        interface Long : Number<KotlinLong> {
            override val defaultValue: KotlinLong get() = 0L
        }

        interface Short : Number<KotlinShort> {
            override val defaultValue: KotlinShort get() = 0
        }

        interface Factory {

            fun byte(
                key: String,
                defaultValue: KotlinByte = 0,
                placeholder: KotlinAny? = null,
                isRequired: KotlinBoolean = false,
            ): Byte

            fun double(
                key: String,
                defaultValue: KotlinDouble = 0.0,
                placeholder: KotlinAny? = null,
                isRequired: KotlinBoolean = false,
            ): Double

            fun float(
                key: String,
                defaultValue: KotlinFloat = 0.0F,
                placeholder: KotlinAny? = null,
                isRequired: KotlinBoolean = false,
            ): Float

            fun int(
                key: String,
                defaultValue: KotlinInt = 0,
                placeholder: KotlinAny? = null,
                isRequired: KotlinBoolean = false,
            ): Int

            fun long(
                key: String,
                defaultValue: KotlinLong = 0L,
                placeholder: KotlinAny? = null,
                isRequired: KotlinBoolean = false,
            ): Long

            fun short(
                key: String,
                defaultValue: KotlinShort = 0,
                placeholder: KotlinAny? = null,
                isRequired: KotlinBoolean = false,
            ): Short

        }

    }

    /** Fields that offer a fixed set of choices. */
    sealed interface Selectable<T> : ConfigField<T> {

        /** The exhaustive list of values the user may choose from. */
        val variants: List<T>

        /** Enum-backed selection — variants are derived from the enum constants. */
        interface Enum<E : KotlinEnum<E>> : Selectable<E> {
            override val defaultValue: E
        }

        /** String-literal selection — variants are plain strings. */
        interface Literal : Selectable<String> {
            override val defaultValue: String
        }

        interface Factory {

            fun <E : KotlinEnum<E>> enum(
                key: String,
                variants: List<E>,
                defaultValue: E,
                placeholder: KotlinAny? = null,
                isRequired: KotlinBoolean = false,
            ): Enum<E>

            fun literal(
                key: String,
                variants: List<String>,
                defaultValue: String,
                placeholder: KotlinAny? = null,
                isRequired: KotlinBoolean = false,
            ): Literal

        }

    }

    /** Plain-text fields. */
    sealed interface Text : ConfigField<String> {

        /** Plain single-line text with no special rendering. */
        interface Default : Text

        /** Sensitive text that should be masked in the UI. */
        interface Password : Text

        /** A URL, which may be validated and rendered as a link. */
        interface Url : Text

        interface Factory {

            fun default(
                key: String,
                defaultValue: String? = null,
                placeholder: KotlinAny? = null,
                isRequired: KotlinBoolean = false,
            ): Default

            fun password(
                key: String,
                defaultValue: String? = null,
                placeholder: KotlinAny? = null,
                isRequired: KotlinBoolean = false,
            ): Password

            fun url(
                key: String,
                defaultValue: String? = null,
                placeholder: KotlinAny? = null,
                isRequired: KotlinBoolean = false,
            ): Url

        }

    }

    /**
     * Creates [ConfigField] instances.
     *
     * The default implementation is pre-registered in [ConfigSchemaFactories.field].
     * Override it there to customize field construction globally.
     */
    interface Factory {

        val number: Number.Factory

        val selectable: Selectable.Factory

        val text: Text.Factory

        fun boolean(
            key: String,
            defaultValue: KotlinBoolean = false,
            placeholder: KotlinAny? = null,
            required: KotlinBoolean = false,
        ): ConfigField.Boolean

    }

}
