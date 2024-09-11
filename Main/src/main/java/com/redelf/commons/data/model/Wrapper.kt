package com.redelf.commons.data.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

open class Wrapper<T> @JsonCreator constructor(

    @SerializedName("data")
    @JsonProperty("data")
    protected var data: T?

) {

    fun takeData() = data
}