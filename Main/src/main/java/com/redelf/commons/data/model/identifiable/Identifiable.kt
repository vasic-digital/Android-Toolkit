package com.redelf.commons.data.model.identifiable

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap

abstract class Identifiable<I> {

    @JsonProperty("id")
    @SerializedName("id")
    private var id: I? = null

    @JsonCreator constructor()

    @Throws(ClassCastException::class)
    constructor(data: LinkedTreeMap<String, Any>) : this()

    open fun getId(): I? {

        if (id == null) {

            setId(initializeId())
        }

        return id
    }

    fun setId(id: I) {

        this.id = id
    }

    abstract fun hasValidId(): Boolean

    abstract fun initializeId(): I
}
