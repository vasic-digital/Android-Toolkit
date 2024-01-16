package com.redelf.commons.cache

import com.google.gson.annotations.SerializedName

abstract class Cacheable {

    @Transient
    @SerializedName("fromCache")
    var fromCache: Boolean = false
}