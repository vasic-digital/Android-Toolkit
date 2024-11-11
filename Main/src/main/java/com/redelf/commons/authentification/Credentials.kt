package com.redelf.commons.authentification

import android.text.TextUtils
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import com.redelf.commons.data.Empty

class Credentials @JsonCreator constructor(

    @JsonProperty("username")
    @SerializedName("username")
    var username: String? = "",

    @JsonProperty("password")
    @SerializedName("password")
    var password: String? = ""

) : Empty {

    constructor() : this("", "")

    override fun isEmpty(): Boolean = TextUtils.isEmpty(username) || TextUtils.isEmpty(password)

    override fun isNotEmpty() = !isEmpty()

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

    @Suppress("UNCHECKED_CAST")
    @Throws(ClassCastException::class)
    constructor(treeMap: LinkedTreeMap<String, Any>) : this() {

        username = treeMap["username"].toString()
        password = treeMap["password"].toString()
    }
}