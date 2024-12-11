package com.redelf.commons.test.data

import com.fasterxml.jackson.annotation.JsonCreator
import com.google.gson.annotations.SerializedName
import com.redelf.commons.persistance.serialization.CustomSerializable
import com.redelf.commons.persistance.serialization.DefaultCustomSerializer

data class CustomAsset @JsonCreator constructor(

    @SerializedName("bytes")
    var bytes: ByteArray? = null,
    @SerializedName("size")
    var size: Long? = 0L,
    @SerializedName("filename")
    var fileName: String? = "",
    @SerializedName("cid")
    var cid: String? = "",
    @SerializedName("mime")
    var mimeType: String? = ""

) : CustomSerializable {

    constructor() : this(null, 0L, "", "", "")

    override fun getCustomSerializations() = mapOf(

        "bytes" to DefaultCustomSerializer()
    )
}