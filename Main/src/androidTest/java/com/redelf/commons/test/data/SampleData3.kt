package com.redelf.commons.test.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
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
    var points: List<String>? = emptyList()

) {

    constructor() : this(id = UUID.randomUUID())
}
