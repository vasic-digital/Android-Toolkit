package com.redelf.commons.persistance

import com.google.gson.JsonSyntaxException
import com.redelf.commons.extensions.forClassName
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console.error
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.persistance.base.Parser
import com.redelf.commons.persistance.base.Serializer

internal class DataSerializer(private val parser: Obtain<Parser>) : Serializer {

    /*
        TODO: Create a flavor that uses Jackson lib for stream-like serialization / deserialization
    */

    override fun <T> serialize(cipherText: String?, value: T): String? {

        if (cipherText == null || cipherText.isEmpty()) {
            
            return null
        }

        if (value == null) {
            
            return null
        }

        var keyClassName: Class<*>? = null
        var valueClassName: Class<*>? = null

        var dataType: String

        if (MutableList::class.java.isAssignableFrom(value.javaClass)) {
            
            val list = value as MutableList<*>
            
            if (!list.isEmpty()) {

                keyClassName = list.get(0)?.javaClass
            }

            dataType = DataInfo.TYPE_LIST

        } else if (MutableMap::class.java.isAssignableFrom(value.javaClass)) {

            dataType = DataInfo.TYPE_MAP

            val map = value as MutableMap<*, *>

            if (!map.isEmpty()) {

                for (entry in map.entries) {

                    keyClassName = entry.key?.javaClass
                    valueClassName = entry.value?.javaClass
                    break
                }

            }

        } else if (MutableSet::class.java.isAssignableFrom(value.javaClass)) {

            val set = value as MutableSet<*>

            if (!set.isEmpty()) {

                val iterator: MutableIterator<*> = set.iterator()

                if (iterator.hasNext()) {

                    keyClassName = iterator.next()?.javaClass
                }
            }

            dataType = DataInfo.TYPE_SET

        } else {

            dataType = DataInfo.TYPE_OBJECT
            keyClassName = value.javaClass
        }

        val dataInfo = DataInfo(

            cipherText,
            dataType,
            keyClassName?.name,
            valueClassName?.name,
            keyClassName?.canonicalName?.forClassName(),
            valueClassName?.canonicalName?.forClassName()
        )

        try {

            return parser.obtain().toJson(dataInfo)

        } catch (e: OutOfMemoryError) {

            recordException(e)

        } catch (e: Throwable) {

            error(e)
        }

        return null
    }

    override fun deserialize(plainText: String?): DataInfo? {

        if (isEmpty(plainText)) {

            return null
        }

        try {

            val dataInfo = parser.obtain().fromJson<DataInfo?>(plainText, DataInfo::class.java)

            if (dataInfo?.keyClazzName != null) {

                dataInfo.keyClazz = dataInfo.keyClazzName?.forClassName()
            }

            if (dataInfo?.valueClazzName != null) {

                dataInfo.valueClazz = dataInfo.valueClazzName?.forClassName()
            }

            return dataInfo

        } catch (e: JsonSyntaxException) {

            error("Could not deserialize: $plainText")

            error(e)
        }

        return null
    }
}
