package com.redelf.commons.sorting

import com.google.gson.annotations.SerializedName

open class SortingParameters(

    @SerializedName("direction")
    val direction: SortingDirection = SortingDirection.ASCENDING

) {

    override fun toString(): String {

        return "SortingParameters(direction=$direction)"
    }
}