/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.redelf.commons.test.test_data

import com.fasterxml.jackson.annotation.JsonCreator
import com.google.gson.annotations.SerializedName
import com.redelf.commons.persistance.serialization.CustomSerializable
import com.redelf.commons.persistance.serialization.DefaultCustomSerializer

open class CustomAsset @JsonCreator constructor(

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

        "bytes" to DefaultCustomSerializer(ByteArray::class.java)
    )
}