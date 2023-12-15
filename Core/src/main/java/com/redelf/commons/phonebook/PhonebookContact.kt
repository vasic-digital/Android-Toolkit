package com.redelf.commons.phonebook

import android.net.Uri
import com.google.gson.annotations.SerializedName

data class PhonebookContact(

    @SerializedName("name")
    val name: String,
    @SerializedName("email")
    val email: ArrayList<String> = ArrayList(),
    @SerializedName("phone")
    val phone: ArrayList<String> = ArrayList(),
    @SerializedName("avatar")
    val avatar: Uri? = null
) {

    fun getId() = "$email$phone".hashCode().toLong()
}