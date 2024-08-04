package com.redelf.commons.net.retrofit

import android.content.Context
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import okhttp3.Call
import java.util.concurrent.ConcurrentHashMap

data class RetrofitApiParameters @JsonCreator constructor(

    @JsonIgnore
    @Transient
    @JsonProperty("ctx")
    @SerializedName("ctx")
    val ctx: Context,

    @JsonProperty("readTimeoutInSeconds")
    @SerializedName("readTimeoutInSeconds")
    val readTimeoutInSeconds: Long? = 10,

    @JsonProperty("connectTimeoutInSeconds")
    @SerializedName("connectTimeoutInSeconds")
    val connectTimeoutInSeconds: Long? = 10,

    @JsonProperty("writeTimeoutInSeconds")
    @SerializedName("writeTimeoutInSeconds")
    val writeTimeoutInSeconds: Long? = -1,

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

    @JsonProperty("callsWrapper")
    @SerializedName("callsWrapper")
    val callsWrapper: ConcurrentHashMap<String, Call>? = GlobalCallsWrapper.CALLS
)