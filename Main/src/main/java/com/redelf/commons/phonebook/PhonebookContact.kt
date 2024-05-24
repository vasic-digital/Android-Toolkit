package com.redelf.commons.phonebook

import android.net.Uri
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

data class PhonebookContact @JsonCreator constructor(

    @JsonProperty("name")
    @SerializedName("name")
    val name: String? = null,

    @JsonProperty("email")
    @SerializedName("email")
    val email: ArrayList<String>? = ArrayList(),

    @JsonProperty("phone")
    @SerializedName("phone")
    val phone: ArrayList<String>? = ArrayList(),

    @JsonProperty("avatar")
    @SerializedName("avatar")
    val avatar: Uri? = null

) {

    fun getId() = "$email$phone".hashCode().toLong()
}