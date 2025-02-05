package com.redelf.commons.test.test_data

import com.google.gson.annotations.SerializedName

class ExtendedCustomAsset(

    bytes: ByteArray? = null,
    size: Long? = 0L,
    fileName: String? = "",
    cid: String? = "",
    mimeType: String? = "",

    @SerializedName("custom_asset")
    val customAsset: CustomAsset? = null,
    @SerializedName("custom_asset")
    val customAssets: List<CustomAsset>? = null

) : CustomAsset(

    bytes, size, fileName, cid, mimeType
)