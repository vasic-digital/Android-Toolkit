package com.redelf.commons.test.test_data

import com.fasterxml.jackson.annotation.JsonCreator
import com.google.gson.annotations.SerializedName

data class SimpleAsset @JsonCreator constructor(

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

) {

    constructor() : this(null, 0L, "", "", "")
}