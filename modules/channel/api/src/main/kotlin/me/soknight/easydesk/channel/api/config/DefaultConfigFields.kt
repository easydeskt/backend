package me.soknight.easydesk.channel.api.config

import kotlin.Any as KotlinAny
import kotlin.Boolean as KotlinBoolean
import kotlin.Byte as KotlinByte
import kotlin.Double as KotlinDouble
import kotlin.Enum as KotlinEnum
import kotlin.Float as KotlinFloat
import kotlin.Int as KotlinInt
import kotlin.Long as KotlinLong
import kotlin.Short as KotlinShort

internal data class DefaultBoolean(
    override val key: String,
    override val defaultValue: KotlinBoolean = false,
    override val isRequired: KotlinBoolean = false,
    override val placeholder: String? = null,
) : ConfigField.Boolean

internal data class DefaultByte(
    override val key: String,
    override val defaultValue: KotlinByte = 0,
    override val isRequired: KotlinBoolean = false,
    override val placeholder: String? = null,
) : ConfigField.Number.Byte

internal data class DefaultDouble(
    override val key: String,
    override val defaultValue: KotlinDouble = 0.0,
    override val isRequired: KotlinBoolean = false,
    override val placeholder: String? = null,
) : ConfigField.Number.Double

internal data class DefaultEnum<E : KotlinEnum<E>>(
    override val key: String,
    override val variants: List<E>,
    override val defaultValue: E,
    override val isRequired: KotlinBoolean = false,
    override val placeholder: String? = null,
) : ConfigField.Selectable.Enum<E>

internal data class DefaultFloat(
    override val key: String,
    override val defaultValue: KotlinFloat = 0.0F,
    override val isRequired: KotlinBoolean = false,
    override val placeholder: String? = null,
) : ConfigField.Number.Float

internal data class DefaultInt(
    override val key: String,
    override val defaultValue: KotlinInt = 0,
    override val isRequired: KotlinBoolean = false,
    override val placeholder: String? = null,
) : ConfigField.Number.Int

internal data class DefaultLiteral(
    override val key: String,
    override val variants: List<String>,
    override val defaultValue: String,
    override val isRequired: KotlinBoolean = false,
    override val placeholder: String? = null,
) : ConfigField.Selectable.Literal

internal data class DefaultLong(
    override val key: String,
    override val defaultValue: KotlinLong = 0L,
    override val isRequired: KotlinBoolean = false,
    override val placeholder: String? = null,
) : ConfigField.Number.Long

internal data class DefaultPassword(
    override val key: String,
    override val defaultValue: String? = null,
    override val isRequired: KotlinBoolean = false,
    override val placeholder: String? = null,
) : ConfigField.Text.Password

internal data class DefaultShort(
    override val key: String,
    override val defaultValue: KotlinShort = 0,
    override val isRequired: KotlinBoolean = false,
    override val placeholder: String? = null,
) : ConfigField.Number.Short

internal data class DefaultText(
    override val key: String,
    override val defaultValue: String? = null,
    override val isRequired: KotlinBoolean = false,
    override val placeholder: String? = null,
) : ConfigField.Text.Default

internal data class DefaultUrl(
    override val key: String,
    override val defaultValue: String? = null,
    override val isRequired: KotlinBoolean = false,
    override val placeholder: String? = null,
) : ConfigField.Text.Url

internal object DefaultNumberFactory : ConfigField.Number.Factory {

    override fun byte(
        key: String,
        defaultValue: KotlinByte,
        placeholder: KotlinAny?,
        isRequired: KotlinBoolean,
    ) = DefaultByte(key, defaultValue, isRequired, placeholder?.toString())

    override fun double(
        key: String,
        defaultValue: KotlinDouble,
        placeholder: KotlinAny?,
        isRequired: KotlinBoolean,
    ) = DefaultDouble(key, defaultValue, isRequired, placeholder?.toString())

    override fun float(
        key: String,
        defaultValue: KotlinFloat,
        placeholder: KotlinAny?,
        isRequired: KotlinBoolean,
    ) = DefaultFloat(key, defaultValue, isRequired, placeholder?.toString())

    override fun int(
        key: String,
        defaultValue: KotlinInt,
        placeholder: KotlinAny?,
        isRequired: KotlinBoolean,
    ) = DefaultInt(key, defaultValue, isRequired, placeholder?.toString())

    override fun long(
        key: String,
        defaultValue: KotlinLong,
        placeholder: KotlinAny?,
        isRequired: KotlinBoolean,
    ) = DefaultLong(key, defaultValue, isRequired, placeholder?.toString())

    override fun short(
        key: String,
        defaultValue: KotlinShort,
        placeholder: KotlinAny?,
        isRequired: KotlinBoolean,
    ) = DefaultShort(key, defaultValue, isRequired, placeholder?.toString())

}

internal object DefaultSelectableFactory : ConfigField.Selectable.Factory {

    override fun <E : KotlinEnum<E>> enum(
        key: String,
        variants: List<E>,
        defaultValue: E,
        placeholder: KotlinAny?,
        isRequired: KotlinBoolean,
    ) = DefaultEnum(key, variants, defaultValue, isRequired, placeholder?.toString())

    override fun literal(
        key: String,
        variants: List<String>,
        defaultValue: String,
        placeholder: KotlinAny?,
        isRequired: KotlinBoolean,
    ) = DefaultLiteral(key, variants, defaultValue, isRequired, placeholder?.toString())

}

internal object DefaultTextFactory : ConfigField.Text.Factory {

    override fun default(
        key: String,
        defaultValue: String?,
        placeholder: KotlinAny?,
        isRequired: KotlinBoolean,
    ) = DefaultText(key, defaultValue, isRequired, placeholder?.toString())

    override fun password(
        key: String,
        defaultValue: String?,
        placeholder: KotlinAny?,
        isRequired: KotlinBoolean,
    ) = DefaultPassword(key, defaultValue, isRequired, placeholder?.toString())

    override fun url(
        key: String,
        defaultValue: String?,
        placeholder: KotlinAny?,
        isRequired: KotlinBoolean,
    ) = DefaultUrl(key, defaultValue, isRequired, placeholder?.toString())

}

internal object DefaultConfigFieldFactory : ConfigField.Factory {

    override val number: ConfigField.Number.Factory = DefaultNumberFactory

    override val selectable: ConfigField.Selectable.Factory = DefaultSelectableFactory

    override val text: ConfigField.Text.Factory = DefaultTextFactory

    override fun boolean(
        key: String,
        defaultValue: KotlinBoolean,
        placeholder: KotlinAny?,
        required: KotlinBoolean,
    ) = DefaultBoolean(key, defaultValue, required, placeholder?.toString())

}
