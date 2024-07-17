package com.redelf.commons.data.model.identifiable

import com.fasterxml.jackson.annotation.JsonCreator
import com.google.gson.internal.LinkedTreeMap

abstract class IdentifiableLong : Identifiable<Long> {

    @JsonCreator constructor() : super()

    @Throws(ClassCastException::class)
    constructor(data: LinkedTreeMap<String, Any>) : super(data) {

        val toSet = (data["id"] as Double).toLong()

        setId(toSet)
    }

    override fun hasValidId(): Boolean {

        return getId() != null && getId() != 0L
    }

    override fun initializeId(): Long {

        return 0
    }
}
