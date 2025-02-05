package com.redelf.commons.sorting

import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap

open class SortingParameters(

    @JsonProperty("direction")
    @SerializedName("direction")
    var direction: SortingDirection? = SortingDirection.ASCENDING

) {

    constructor() : this(SortingDirection.ASCENDING)

    override fun toString(): String {

        return "SortingParameters(direction=$direction)"
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(ClassCastException::class)
    constructor(treeMap: LinkedTreeMap<String, Any>) : this() {

        val sVal = treeMap["direction"].toString()
        direction = SortingDirection.fromString(sVal) ?: SortingDirection.ASCENDING
    }
}