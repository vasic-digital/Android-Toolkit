package com.redelf.commons.search

import com.fasterxml.jackson.annotation.JsonCreator
import com.google.gson.annotations.SerializedName

data class SearchResult<out T> @JsonCreator constructor(@SerializedName("result") val result: T)