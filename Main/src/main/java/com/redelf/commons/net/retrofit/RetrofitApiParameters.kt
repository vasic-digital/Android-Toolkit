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


package com.redelf.commons.net.retrofit

import android.content.Context
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import okhttp3.Call
import java.util.concurrent.ConcurrentHashMap

data class RetrofitApiParameters @JsonCreator constructor(

    @JsonProperty("name")
    @SerializedName("name")
    val name: String? = "",

    @JsonIgnore
    @Transient
    @JsonProperty("ctx")
    @SerializedName("ctx")
    val ctx: Context,

    @JsonProperty("readTimeoutInSeconds")
    @SerializedName("readTimeoutInSeconds")
    val readTimeoutInSeconds: Long? = 30,

    @JsonProperty("connectTimeoutInSeconds")
    @SerializedName("connectTimeoutInSeconds")
    val connectTimeoutInSeconds: Long? = 30,

    @JsonProperty("writeTimeoutInSeconds")
    @SerializedName("writeTimeoutInSeconds")
    val writeTimeoutInSeconds: Long? = 30,

    @JsonProperty("endpoint")
    @SerializedName("endpoint")
    val endpoint: Int,

    @JsonProperty("scalar")
    @SerializedName("scalar")
    val scalar: Boolean? = false,

    @JsonProperty("jackson")
    @SerializedName("jackson")
    val jackson: Boolean? = false,

    @JsonProperty("bodyLog")
    @SerializedName("bodyLog")
    var bodyLog: Boolean? = true,

    @JsonProperty("bodyLog")
    @SerializedName("bodyLog")
    var verbose: Boolean? = false,

    @JsonProperty("useCronet")
    @SerializedName("useCronet")
    var useCronet: Boolean? = true,

    @JsonProperty("callsWrapper")
    @SerializedName("callsWrapper")
    val callsWrapper: ConcurrentHashMap<String, Call>? = GlobalCallsWrapper.CALLS
)