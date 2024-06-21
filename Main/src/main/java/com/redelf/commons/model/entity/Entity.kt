package com.redelf.commons.model.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.google.gson.internal.LinkedTreeMap
import com.redelf.commons.model.Identifiable
import com.redelf.commons.obtain.Obtain
import java.io.Serializable

abstract class Entity : Identifiable, Serializable {

    @JsonCreator constructor() : super()

    @Throws(ClassCastException::class)
    constructor(data: LinkedTreeMap<String, Any>) : super(data)


    override fun initializeId(): Long {

        return idProvider().generateNewId()
    }

    protected abstract fun getEntityKind(): Obtain<String>

    private fun idProvider(): EntityIdProvide = EntityIdProvider(getEntityKind())
}
