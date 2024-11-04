package com.redelf.commons.authentification

import android.text.TextUtils
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

data class Credentials @JsonCreator constructor(

    @JsonProperty("username")
    @SerializedName("username")
    val username: String? = "",

    @JsonProperty("password")
    @SerializedName("password")
    val password: String? = ""

) {

    // TODO: SMail - Data mapping

    fun isEmpty(): Boolean = TextUtils.isEmpty(username) || TextUtils.isEmpty(password)
    override fun equals(other: Any?): Boolean {

        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Credentials

        if (username != other.username) return false
        return password == other.password
    }

    override fun hashCode(): Int {

        var result = username.hashCode()
        result = 31 * result + password.hashCode()
        return result
    }
}