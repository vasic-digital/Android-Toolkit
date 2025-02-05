package com.redelf.commons.caching

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName

abstract class Cacheable {

    @Transient
    @JsonIgnore
    @JsonProperty("fromCache")
    @SerializedName("fromCache")
    var fromCache: Boolean = false
}