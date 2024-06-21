package com.redelf.commons.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap

/*
* TODO: Abstraction branches and Long and UUID descendants
*/
abstract class Identifiable {

    @JsonProperty("id")
    @SerializedName("id")
    private var id: Long? = null

    @JsonCreator constructor()

    @Throws(ClassCastException::class)
    constructor(data: LinkedTreeMap<String, Any>) : this() {

        id = (data["id"] as Double).toLong()
    }

    open fun getId(): Long? {

        if (id == null) {

            setId(initializeId())
        }

        return id
    }

    fun setId(id: Long) {

        this.id = id
    }

    fun hasValidId(): Boolean {

        return getId() != null && getId() != 0L
    }

    protected open fun initializeId(): Long {

        return 0
    }
}
