/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


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
