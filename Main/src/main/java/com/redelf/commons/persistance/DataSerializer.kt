package com.redelf.commons.persistance

import android.text.TextUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.redelf.commons.extensions.forClassName
import com.redelf.commons.extensions.isEmpty
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console.error
import com.redelf.commons.persistance.base.Serializer
import java.lang.Exception

internal class DataSerializer : Serializer {

    /*
        TODO: Create a flavor that uses Jackson lib for stream-like serialization / deserialization
    */
    private val gsn: Gson = Gson()

    override fun <T> serialize(cipherText: ByteArray?, originalGivenValue: T): String? {

        if (cipherText == null || cipherText.isEmpty()) {
            
            return null
        }

        if (originalGivenValue == null) {
            
            return null
        }

        var keyClassName: Class<*>? = null
        var valueClassName: Class<*>? = null

        var dataType: Char

        if (MutableList::class.java.isAssignableFrom(originalGivenValue.javaClass)) {
            
            val list = originalGivenValue as MutableList<*>
            
            if (!list.isEmpty()) {
                keyClassName = list.get(0)?.javaClass
            }

            dataType = DataInfo.TYPE_LIST

        } else if (MutableMap::class.java.isAssignableFrom(originalGivenValue.javaClass)) {

            dataType = DataInfo.TYPE_MAP

            val map = originalGivenValue as MutableMap<*, *>

            if (!map.isEmpty()) {

                for (entry in map.entries) {

                    keyClassName = entry.key?.javaClass
                    valueClassName = entry.value?.javaClass
                    break
                }

            }

        } else if (MutableSet::class.java.isAssignableFrom(originalGivenValue.javaClass)) {

            val set = originalGivenValue as MutableSet<*>

            if (!set.isEmpty()) {

                val iterator: MutableIterator<*> = set.iterator()

                if (iterator.hasNext()) {

                    keyClassName = iterator.next()?.javaClass
                }
            }

            dataType = DataInfo.TYPE_SET

        } else {

            dataType = DataInfo.TYPE_OBJECT
            keyClassName = originalGivenValue.javaClass
        }

        val dataInfo = DataInfo(

            cipherText,
            dataType,
            keyClassName?.getName(),
            valueClassName?.getName(),
            keyClassName,
            valueClassName
        )

        try {

            return gsn.toJson(dataInfo)

        } catch (e: OutOfMemoryError) {

            recordException(e)

        } catch (e: Exception) {

            error(e)
        }

        return null
    }

    override fun deserialize(serializedText: String?): DataInfo? {

        if (isEmpty(serializedText)) {

            return null
        }

        try {
            val dataInfo = gsn.fromJson<DataInfo>(serializedText, DataInfo::class.java)

            if (dataInfo.keyClazzName != null) {

                try {

                    dataInfo.keyClazz = Class.forName(dataInfo.keyClazzName.forClassName())

                } catch (e: ClassNotFoundException) {

                    error(e)
                }
            }

            if (dataInfo.valueClazzName != null) {

                try {

                    dataInfo.valueClazz = Class.forName(dataInfo.valueClazzName.forClassName())

                } catch (e: ClassNotFoundException) {

                    error(e)
                }
            }

            return dataInfo

        } catch (e: JsonSyntaxException) {

            error("Could not deserialize: $serializedText")
            error(e)
        }

        return null
    }
}
