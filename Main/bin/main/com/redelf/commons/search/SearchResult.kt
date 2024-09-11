package com.redelf.commons.search

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

data class SearchResult<out T> @JsonCreator constructor(

    @JsonProperty("result")
    @SerializedName("result") val result: T
)