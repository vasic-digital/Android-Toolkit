package com.redelf.commons.test.test_data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import java.util.UUID

data class SampleData3 @JsonCreator constructor(

    @JsonProperty("id")
    @SerializedName("id")
    var id: UUID,

    @JsonProperty("title")
    @SerializedName("title")
    var title: String? = "",

    @JsonProperty("order")
    @SerializedName("order")
    var order: Long? = 0,

    @JsonProperty("points")
    @SerializedName("points")
    var points: MutableList<String>? = mutableListOf()

) {

    constructor() : this(id = UUID.randomUUID())

    @Suppress("UNCHECKED_CAST")
    @Throws(ClassCastException::class)
    constructor(treeMap: LinkedTreeMap<String, Any>) : this(

        id = UUID.fromString(treeMap["id"].toString()),
        title = treeMap["title"].toString(),
        order = (treeMap["order"] as Double).toLong(),
        points = treeMap["points"] as MutableList<String>
    )
}
