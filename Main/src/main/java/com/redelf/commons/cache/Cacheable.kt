package com.redelf.commons.cache

import com.fasterxml.jackson.annotation.JsonIgnore
import com.google.gson.annotations.SerializedName

abstract class Cacheable {

    @Transient
    @JsonIgnore
    @SerializedName("fromCache")
    var fromCache: Boolean = false
}