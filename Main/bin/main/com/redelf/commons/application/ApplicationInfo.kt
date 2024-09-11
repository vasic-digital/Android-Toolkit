package com.redelf.commons.application

interface ApplicationInfo {

    fun getVersion(): String

    fun getVersionCode(): String

    fun getName(): String
}