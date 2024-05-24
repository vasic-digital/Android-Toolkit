package com.redelf.commons.persistance

import com.fasterxml.jackson.annotation.JsonIgnore

class DataInfo(

    val cipherText: ByteArray?,

    val dataType: Char,
    val keyClazzName: String?,
    val valueClazzName: String?,

    @JsonIgnore
    @Transient var keyClazz: Class<*>?,

    @JsonIgnore
    @Transient var valueClazz: Class<*>?

) {
    companion object {

        const val TYPE_OBJECT = '0'
        const val TYPE_LIST = '1'
        const val TYPE_MAP = '2'
        const val TYPE_SET = '3'
    }
}