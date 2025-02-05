package com.redelf.commons.persistance

import com.google.gson.reflect.TypeToken
import com.redelf.commons.extensions.recordException
import com.redelf.commons.extensions.toClass
import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.suspendable.Obtain
import com.redelf.commons.persistance.base.Converter
import com.redelf.commons.persistance.base.Parser
import java.lang.reflect.Type

@Suppress("UNCHECKED_CAST")
internal class DataConverter(private val parser: Obtain<Parser>) : Converter {

    private val tag = "Converter :: Data ::"

    private fun debug() = Converter.Companion.DEBUG

    override fun <T> toString(value: T): String? {

        if (value == null) {

            return null
        }

        if (value is String) {

            return value
        }

        if (debug().get()) Console.log("$tag START :: Class = ${value::class.java.canonicalName}")

        val p = parser.obtain()

        return p.toJson(value)
    }

    override fun <T> fromString(value: String?, info: DataInfo?): T? {

        if (value == null) {

            return null
        }

        if (info == null) {

            return null
        }

        val keyType = info.keyClazz
        val valueType = info.valueClazz

        try {

            return when (info.dataType) {

                DataInfo.TYPE_OBJECT -> toObject<T>(value, keyType?.toClass())
                DataInfo.TYPE_LIST -> toList<T>(value, keyType?.toClass())
                DataInfo.TYPE_MAP -> toMap<Any, Any, T>(value, keyType?.toClass(), valueType?.toClass())
                DataInfo.TYPE_SET -> toSet<T>(value, keyType?.toClass())

                else -> null
            }
        } catch (e: Exception) {

            recordException(e)
        }

        return null
    }

    override fun <T> fromString(value: String?, type: Type?): T? {

        if (value == null) {

            return null
        }

        if (type == null) {

            return null
        }

        val p = parser.obtain()

        try {

            return p.fromJson(value, type)

        } catch (e: Exception) {

            Console.error(e)

            Console.error("Tried to deserialize into '${type.typeName}' from '$value'")
        }

        return null
    }

    override fun <T> fromString(value: String?, clazz: Class<T>?): T? {

        if (value == null) {

            return null
        }

        if (clazz == null) {

            return null
        }

        val p = parser.obtain()

        try {

            return p.fromJson(value, clazz)

        } catch (e: Exception) {

            Console.error(e)

            Console.error("Tried to deserialize into '${clazz.simpleName}' from '$value'")
        }

        return null
    }

    @Throws(Exception::class)
    private fun <T> toObject(json: String, type: Class<*>?): T? {

        val p = parser.obtain()

        return p.fromJson<T>(json, type)
    }

    @Throws(Exception::class)
    private fun <T> toList(json: String, type: Class<*>?): T {

        if (type == null) {

            return ArrayList<Any>() as T
        }

        val p = parser.obtain()

        val list = p.fromJson<MutableList<T?>>(

            json,
            object : TypeToken<List<T>?>() {
            }.type

        )

        if (list == null) {

            return ArrayList<Any>() as T
        }

        val size = list.size

        for (i in 0 until size) {

            list[i] = p.fromJson<Any>(p.toJson(list[i]), type) as T?
        }

        return list as T
    }

    @Throws(Exception::class)
    private fun <T> toSet(json: String, type: Class<*>?): T? {

        val resultSet: MutableSet<T?> = HashSet()

        if (type == null) {

            return resultSet as T
        }

        val p = parser.obtain()

        val set = p.fromJson<Set<T>?>(json, object : TypeToken<Set<T>?>() {}.type) ?: return resultSet as T

        for (t in set) {

            val valueJson = p.toJson(t)
            val value = p.fromJson<T>(valueJson, type)

            resultSet.add(value)
        }

        return resultSet as T
    }

    @Throws(Exception::class)
    private fun <K, V, T> toMap(json: String, keyType: Class<*>?, valueType: Class<*>?): T? {

        val resultMap: MutableMap<K?, V?> = HashMap()

        if (keyType == null || valueType == null) {

            return resultMap as T
        }

        val p = parser.obtain()

        val map = p.fromJson<Map<K, V>>(json, object : TypeToken<Map<K, V>?>() {}.type) ?: return resultMap as T

        for ((key, value) in map) {

            val keyJson = p.toJson(key)
            val k = p.fromJson<K>(keyJson, keyType)

            val valueJson = p.toJson(value)
            val v = p.fromJson<V>(valueJson, valueType)

            resultMap[k] = v
        }

        return resultMap as T
    }
}
