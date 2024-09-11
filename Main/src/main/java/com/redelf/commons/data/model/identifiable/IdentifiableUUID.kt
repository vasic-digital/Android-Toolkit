package com.redelf.commons.data.model.identifiable

import com.fasterxml.jackson.annotation.JsonCreator
import com.google.gson.internal.LinkedTreeMap
import java.util.UUID

abstract class IdentifiableUUID : Identifiable<UUID> {

    @JsonCreator constructor() : super()

    @Throws(ClassCastException::class)
    constructor(data: LinkedTreeMap<String, Any>) : super(data) {

        val toSet = UUID.fromString(data["id"].toString())

        setId(toSet)
    }

    override fun hasValidId(): Boolean {

        return getId() != null
    }

    override fun initializeId(): UUID {

        return UUID.randomUUID()
    }
}
