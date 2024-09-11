package com.redelf.commons.interprocess

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

data class InterprocessError(

    @JsonProperty("error")
    @SerializedName("error")
    var error: String? = "",

) {

    companion object {

        const val BUNDLE_KEY = "error"
    }
}
