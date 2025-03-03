package com.redelf.commons.interprocess

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

data class InterprocessData(

    @JsonProperty("function")
    @SerializedName("function")
    var function: String? = "",

    @JsonProperty("content")
    @SerializedName("content")
    var content: String? = ""

) {

    companion object {

        const val BUNDLE_KEY = "data"
    }
}
