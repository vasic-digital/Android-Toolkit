package com.redelf.commons.phonebook

import android.net.Uri
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

data class PhonebookContact @JsonCreator constructor(

    @JsonProperty("name")
    @SerializedName("name")
    var name: String? = null,

    @JsonProperty("email")
    @SerializedName("email")
    var email: MutableList<String>? = mutableListOf(),

    @JsonProperty("phone")
    @SerializedName("phone")
    var phone: MutableList<String>? = mutableListOf(),

    @JsonProperty("avatar")
    @SerializedName("avatar")
    var avatar: Uri? = null

) {

    fun getId() = "$email$phone".hashCode().toLong()
}