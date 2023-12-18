package com.redelf.commons

import android.text.TextUtils
import com.google.gson.annotations.SerializedName

data class Credentials(

    @SerializedName("username")
    val username: String = "",

    @SerializedName("password")
    val password: String = ""

) {

    fun isEmpty(): Boolean = TextUtils.isEmpty(username) || TextUtils.isEmpty(password)
}