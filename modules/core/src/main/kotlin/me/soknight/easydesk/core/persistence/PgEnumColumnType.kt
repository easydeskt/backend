package me.soknight.easydesk.core.persistence

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.postgresql.util.PGobject
import kotlin.reflect.KClass

class PgEnumColumnType<T : Enum<T>>(
    private val klass: KClass<T>,
    private val typeName: String,
) : ColumnType<T>() {

    override fun sqlType() = typeName

    @Suppress("UNCHECKED_CAST")
    override fun valueFromDB(value: Any): T = when {
        klass.isInstance(value) -> value as T
        value is PGobject       -> java.lang.Enum.valueOf(klass.java, value.value!!)
        value is String         -> java.lang.Enum.valueOf(klass.java, value)
        else                    -> error("Unexpected value: $value (${value::class})")
    }

    override fun notNullValueToDB(value: T) =
        PGobject().apply { type = typeName; this.value = value.name }

}

inline fun <reified T : Enum<T>> Table.pgEnum(columnName: String, typeName: String): Column<T> =
    registerColumn(columnName, PgEnumColumnType(T::class, typeName))
