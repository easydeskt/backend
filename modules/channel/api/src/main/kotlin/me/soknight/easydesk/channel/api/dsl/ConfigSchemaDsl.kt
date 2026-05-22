package me.soknight.easydesk.channel.api.dsl

import me.soknight.easydesk.channel.api.config.ConfigField
import me.soknight.easydesk.channel.api.config.ConfigSchema
import me.soknight.easydesk.channel.api.config.ConfigSchemaFactories
import me.soknight.easydesk.channel.api.config.ConfigSection
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** DSL scope marker — prevents outer builder methods from being called inside nested blocks. */
@DslMarker
annotation class ChannelConfigDsl

/**
 * Declares the field types available inside [channelConfigSchema] and [SchemaBuilder.section] blocks.
 *
 * This interface is the **spec** — it names every supported field type and its parameters.
 * [SectionBuilder] is the single **implementation**; [SchemaBuilder] delegates to it via `by`.
 */
@ChannelConfigDsl
interface ChannelConfigScope {

    fun boolean(key: String, defaultValue: Boolean = false, placeholder: Any? = null, required: Boolean = false)

    fun byte(key: String, defaultValue: Byte = 0, placeholder: Any? = null, required: Boolean = false)

    fun double(key: String, defaultValue: Double = 0.0, placeholder: Any? = null, required: Boolean = false)

    fun float(key: String, defaultValue: Float = 0.0F, placeholder: Any? = null, required: Boolean = false)

    fun int(key: String, defaultValue: Int = 0, placeholder: Any? = null, required: Boolean = false)

    fun long(key: String, defaultValue: Long = 0L, placeholder: Any? = null, required: Boolean = false)

    fun short(key: String, defaultValue: Short = 0, placeholder: Any? = null, required: Boolean = false)

    fun <E : Enum<E>> enumSelect(key: String, vararg variants: E, defaultValue: E, placeholder: Any? = null, required: Boolean = false)

    fun literalSelect(key: String, vararg variants: String, defaultValue: String, placeholder: Any? = null, required: Boolean = false)

    fun text(key: String, defaultValue: String? = null, placeholder: Any? = null, required: Boolean = false)

    fun password(key: String, defaultValue: String? = null, placeholder: Any? = null, required: Boolean = false)

    fun url(key: String, defaultValue: String? = null, placeholder: Any? = null, required: Boolean = false)

}

/**
 * Builder for a single [ConfigSection].
 *
 * Implements [ChannelConfigScope] — each method appends a typed [ConfigField] to the section.
 * Instances are created implicitly by [SchemaBuilder.section].
 */
class SectionBuilder(private val key: String) : ChannelConfigScope {

    private val fields = mutableListOf<ConfigField.Any>()
    private var order: List<String>? = null

    override fun boolean(key: String, defaultValue: Boolean, placeholder: Any?, required: Boolean) {
        this.fields += ConfigSchemaFactories.field.boolean(key, defaultValue, placeholder, required)
    }

    override fun byte(key: String, defaultValue: Byte, placeholder: Any?, required: Boolean) {
        this.fields += ConfigSchemaFactories.field.number.byte(key, defaultValue, placeholder, required)
    }

    override fun double(key: String, defaultValue: Double, placeholder: Any?, required: Boolean) {
        this.fields += ConfigSchemaFactories.field.number.double(key, defaultValue, placeholder, required)
    }

    override fun float(key: String, defaultValue: Float, placeholder: Any?, required: Boolean) {
        this.fields += ConfigSchemaFactories.field.number.float(key, defaultValue, placeholder, required)
    }

    override fun int(key: String, defaultValue: Int, placeholder: Any?, required: Boolean) {
        this.fields += ConfigSchemaFactories.field.number.int(key, defaultValue, placeholder, required)
    }

    override fun long(key: String, defaultValue: Long, placeholder: Any?, required: Boolean) {
        this.fields += ConfigSchemaFactories.field.number.long(key, defaultValue, placeholder, required)
    }

    override fun short(key: String, defaultValue: Short, placeholder: Any?, required: Boolean) {
        this.fields += ConfigSchemaFactories.field.number.short(key, defaultValue, placeholder, required)
    }

    override fun <E : Enum<E>> enumSelect(key: String, vararg variants: E, defaultValue: E, placeholder: Any?, required: Boolean) {
        this.fields += ConfigSchemaFactories.field.selectable.enum(key, variants.toList(), defaultValue, placeholder, required)
    }

    override fun literalSelect(key: String, vararg variants: String, defaultValue: String, placeholder: Any?, required: Boolean) {
        this.fields += ConfigSchemaFactories.field.selectable.literal(key, variants.toList(), defaultValue, placeholder, required)
    }

    override fun password(key: String, defaultValue: String?, placeholder: Any?, required: Boolean) {
        this.fields += ConfigSchemaFactories.field.text.password(key, defaultValue, placeholder, required)
    }

    override fun text(key: String, defaultValue: String?, placeholder: Any?, required: Boolean) {
        this.fields += ConfigSchemaFactories.field.text.default(key, defaultValue, placeholder, required)
    }

    override fun url(key: String, defaultValue: String?, placeholder: Any?, required: Boolean) {
        this.fields += ConfigSchemaFactories.field.text.url(key, defaultValue, placeholder, required)
    }

    fun order(vararg order: String) {
        this.order = order.toList()
    }

    internal fun build(): ConfigSection =
        ConfigSchemaFactories.section.create(key, fields.toList(), order ?: emptyList())

}

/**
 * Builder for a complete [ConfigSchema].
 *
 * Root-level field calls (e.g. [password], [text]) create an unnamed section that is
 * prepended to the named [section] blocks. Use [section] to group related fields.
 *
 * All field methods are delegated to the internal root [SectionBuilder] — no duplication.
 */
class SchemaBuilder(private val root: SectionBuilder = SectionBuilder("")) : ChannelConfigScope by root {

    private val sections = mutableListOf<ConfigSection>()
    private var order: List<String>? = null

    /** Declares a named section and populates it with the fields defined in [block]. */
    fun section(key: String, block: SectionBuilder.() -> Unit) {
        this.sections += SectionBuilder(key).apply(block).build()
    }

    fun order(vararg order: String) {
        this.order = order.toList()
    }

    internal fun build(): ConfigSchema {
        val rootSection = root.build()
        val allSections = if (rootSection.any()) listOf(rootSection) + sections else sections.toList()
        return ConfigSchemaFactories.schema.create(allSections, order ?: emptyList())
    }

}

/**
 * Lazy [ReadOnlyProperty] delegate that builds a [ConfigSchema] once on first access.
 *
 * Returned by [channelConfigSchema]; use it with `by` on a `val` property:
 * ```kotlin
 * override val configSchema by channelConfigSchema { ... }
 * ```
 */
class ChannelConfigSchemaDelegate(private val init: SchemaBuilder.() -> Unit) : ReadOnlyProperty<Any?, ConfigSchema> {

    private val value by lazy { SchemaBuilder().apply(init).build() }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = value

}

/**
 * Declares a [ConfigSchema] using the type-safe DSL.
 *
 * Typically used as a property delegate on a [me.soknight.easydesk.channel.api.ChannelProvider]:
 * ```kotlin
 * override val configSchema by channelConfigSchema {
 *     password("token", required = true)
 *
 *     section("webhook") {
 *         url("url")
 *         password("secret_token")
 *     }
 * }
 * ```
 *
 * The schema is built lazily on first access and cached for the lifetime of the delegate.
 */
fun channelConfigSchema(init: SchemaBuilder.() -> Unit) = ChannelConfigSchemaDelegate(init)
