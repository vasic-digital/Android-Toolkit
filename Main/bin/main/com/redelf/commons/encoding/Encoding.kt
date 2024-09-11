package com.redelf.commons.encoding

interface Encoding {

    fun encode(what: String, textEncoding: String, encoding: String, charset: String): String
}