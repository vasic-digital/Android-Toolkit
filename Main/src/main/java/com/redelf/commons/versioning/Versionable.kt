package com.redelf.commons.versioning

interface Versionable {

    fun getVersion(): Long

    fun increaseVersion(): Long
}