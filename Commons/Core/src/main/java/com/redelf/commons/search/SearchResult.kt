package com.redelf.commons.search

import com.google.gson.annotations.SerializedName

data class SearchResult<out T>(@SerializedName("result") val result: T)