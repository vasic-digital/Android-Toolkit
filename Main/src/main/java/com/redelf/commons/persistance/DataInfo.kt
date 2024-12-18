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