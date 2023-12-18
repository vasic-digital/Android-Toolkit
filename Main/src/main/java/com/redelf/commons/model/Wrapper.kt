package com.redelf.commons.model

import com.google.gson.annotations.SerializedName

open class Wrapper<out T>(@SerializedName("data") private val data: T) {

    fun getData() = data
}