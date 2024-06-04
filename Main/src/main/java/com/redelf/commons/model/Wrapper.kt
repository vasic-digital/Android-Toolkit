package com.redelf.commons.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

open class Wrapper<out T> @JsonCreator constructor(@SerializedName("data") @JsonProperty("data") private val data: T) {

    fun getData() = data
}