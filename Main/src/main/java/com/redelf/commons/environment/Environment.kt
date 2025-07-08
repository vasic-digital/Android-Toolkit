package com.redelf.commons.environment

interface Environment {

    companion object {

        const val DEFAULT = "default"
    }

    fun getEnvironment(): String
}