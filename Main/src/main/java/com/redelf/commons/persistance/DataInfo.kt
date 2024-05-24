package com.redelf.commons.persistance

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

class DataInfo(

    val cipherText: ByteArray?,

    val dataType: Char,
    val keyClazzName: String?,
    val valueClazzName: String?,

    @JsonIgnore
    @JsonProperty("keyClass")
    @SerializedName("keyClass")
    @Transient var keyClazz: Class<*>?,

    @JsonIgnore
    @JsonProperty("valueClass")
    @SerializedName("valueClass")
    @Transient var valueClazz: Class<*>?

) {
    companion object {

        const val TYPE_OBJECT = '0'
        const val TYPE_LIST = '1'
        const val TYPE_MAP = '2'
        const val TYPE_SET = '3'
    }
}