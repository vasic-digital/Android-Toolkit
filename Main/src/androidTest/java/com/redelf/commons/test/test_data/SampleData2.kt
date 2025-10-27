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


package com.redelf.commons.test.test_data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

data class SampleData2 @JsonCreator constructor(

    @JsonProperty("id")
    @SerializedName("id")
    var id: UUID,

    @JsonProperty("isEnabled")
    @SerializedName("isEnabled")
    var isEnabled: Boolean = false,

    @JsonProperty("order")
    @SerializedName("order")
    var order: Long? = 0,

    @JsonProperty("title")
    @SerializedName("title")
    var title: String? = "",

    @JsonProperty("nested")
    @SerializedName("nested")
    var nested: CopyOnWriteArrayList<SampleData3>? = CopyOnWriteArrayList()

) {

    companion object {

        private fun convert(what: ArrayList<LinkedTreeMap<String, Any>>) : CopyOnWriteArrayList<SampleData3> {

            val list = CopyOnWriteArrayList<SampleData3>()

            what.forEach {

                val instance = SampleData3(it)

                list.add(instance)
            }

            return list
        }
    }

    constructor() : this(id = UUID.randomUUID())

    @Suppress("UNCHECKED_CAST")
    @Throws(ClassCastException::class)
    constructor(treeMap: LinkedTreeMap<String, Any>) : this(

        id = UUID.fromString(treeMap["id"].toString()),
        isEnabled = treeMap["isEnabled"] as Boolean,
        order = (treeMap["order"] as Double).toLong(),
        title = treeMap["title"].toString(),
        nested = convert(treeMap["nested"] as ArrayList<LinkedTreeMap<String, Any>>)
    )
}
