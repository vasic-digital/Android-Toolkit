package com.redelf.commons.net.retrofit

import android.content.Context
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import okhttp3.Call
import okhttp3.Dns
import okhttp3.Interceptor
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
    val callsWrapper: ConcurrentHashMap<String, Call>? = GlobalCallsWrapper.CALLS,

    @JsonIgnore
    @Transient
    val additionalInterceptors: List<Interceptor>? = null,

    @JsonIgnore
    @Transient
    val dns: Dns? = null,

    @JsonProperty("retryOnConnectionFailure")
    @SerializedName("retryOnConnectionFailure")
    val retryOnConnectionFailure: Boolean? = true,

    @JsonProperty("callTimeoutInSeconds")
    @SerializedName("callTimeoutInSeconds")
    val callTimeoutInSeconds: Long? = null
)