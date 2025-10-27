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


package com.redelf.commons.authentification

import android.text.TextUtils
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import com.redelf.commons.data.Empty
import com.redelf.commons.versioning.DataVersion

data class Credentials @JsonCreator constructor(

    @JsonProperty("username")
    @SerializedName("username")
    var username: String? = "",

    @JsonProperty("password")
    @SerializedName("password")
    var password: String? = "",

) : Empty, DataVersion() {

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

    override fun toString(): String {

        return "Credentials(username=$username, password=$password)"
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(ClassCastException::class)
    constructor(treeMap: LinkedTreeMap<String, Any>) : this() {

        username = treeMap["username"].toString()
        password = treeMap["password"].toString()
    }
}