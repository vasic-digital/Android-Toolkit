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

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap

data class DataInfo(

    @JsonProperty("cipherText")
    @SerializedName("cipherText")
    var cipherText: String? = null,

    @JsonProperty("dataType")
    @SerializedName("dataType")
    var dataType: String? = null,

    @JsonProperty("keyClazzName")
    @SerializedName("keyClazzName")
    var keyClazzName: String? = null,

    @JsonProperty("valueClazzName")
    @SerializedName("valueClazzName")
    var valueClazzName: String? = null,

    @JsonProperty("keyClazz")
    @SerializedName("keyClazz")
    var keyClazz: String? = null,

    @JsonProperty("valueClazz")
    @SerializedName("valueClazz")
    var valueClazz: String? = null

) {

    companion object {

        const val TYPE_OBJECT: String = 0.toString()
        const val TYPE_LIST: String = 1.toString()
        const val TYPE_MAP: String = 2.toString()
        const val TYPE_SET: String = 3.toString()
    }

    constructor() : this(null, null, null, null, null, null)

    @Suppress("UNCHECKED_CAST")
    @Throws(ClassCastException::class)
    constructor(treeMap: LinkedTreeMap<String, Any>) : this() {

        treeMap["valueClazz"]?.let {

            valueClazz = it.toString()
        }

        treeMap["keyClazz"]?.let {

            keyClazz = it.toString()
        }

        treeMap["valueClazzName"]?.let {

            valueClazzName = it.toString()
        }

        treeMap["keyClazzName"]?.let {

            keyClazzName = it.toString()
        }

        treeMap["dataType"]?.let {

            dataType = it.toString()
        }

        treeMap["cipherText"]?.let {

            throw IllegalStateException("Not implemented yet")
        }
    }
}