package com.redelf.commons.encoding

interface Decoding {

    fun decode(what: String, textEncoding: String, encoding: String, charset: String): String
}