package com.redelf.commons.test.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import java.util.UUID
import java.util.concurrent.CopyOnWriteArrayList

data class SampleData2 @JsonCreator constructor(

    @JsonProperty("id")
    @SerializedName("id")
    var id: UUID,

    @JsonProperty("isEnabled")
    @SerializedName("isEnabled")
    var isEnabled: Boolean = false,

    @JsonProperty("order")
    @SerializedName("order")
    var order: Long? = 0,

    @JsonProperty("title")
    @SerializedName("title")
    var title: String? = "",

    @JsonProperty("nested")
    @SerializedName("nested")
    var nested: CopyOnWriteArrayList<SampleData3>? = CopyOnWriteArrayList()

) {

    constructor() : this(id = UUID.randomUUID())
}
