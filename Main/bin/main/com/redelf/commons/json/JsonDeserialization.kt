package com.redelf.commons.json

interface JsonDeserialization<T> {

    fun fromJson(json: String): T
}