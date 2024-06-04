package com.redelf.commons.sorting

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

open class SortingParameters(

    @JsonProperty("direction")
    @SerializedName("direction")
    val direction: SortingDirection = SortingDirection.ASCENDING

) {

    override fun toString(): String {

        return "SortingParameters(direction=$direction)"
    }
}