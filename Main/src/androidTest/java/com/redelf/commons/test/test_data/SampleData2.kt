package com.redelf.commons.test.test_data

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
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

    companion object {

        private fun convert(what: ArrayList<LinkedTreeMap<String, Any>>) : CopyOnWriteArrayList<SampleData3> {

            val list = CopyOnWriteArrayList<SampleData3>()

            what.forEach {

                val instance = SampleData3(it)

                list.add(instance)
            }

            return list
        }
    }

    constructor() : this(id = UUID.randomUUID())

    @Suppress("UNCHECKED_CAST")
    @Throws(ClassCastException::class)
    constructor(treeMap: LinkedTreeMap<String, Any>) : this(

        id = UUID.fromString(treeMap["id"].toString()),
        isEnabled = treeMap["isEnabled"] as Boolean,
        order = (treeMap["order"] as Double).toLong(),
        title = treeMap["title"].toString(),
        nested = convert(treeMap["nested"] as ArrayList<LinkedTreeMap<String, Any>>)
    )
}
