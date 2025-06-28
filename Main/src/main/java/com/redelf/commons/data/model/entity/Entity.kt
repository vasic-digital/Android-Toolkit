package com.redelf.commons.data.model.entity

import com.fasterxml.jackson.annotation.JsonCreator
import com.google.gson.internal.LinkedTreeMap
import com.redelf.commons.data.model.identifiable.IdentifiableLong
import com.redelf.commons.obtain.Obtain
import java.io.Serializable

abstract class Entity : IdentifiableLong, Serializable {

    @JsonCreator constructor() : super()

    @Throws(ClassCastException::class)
    constructor(data: LinkedTreeMap<String, Any>) : super(data)

    @Synchronized
    override fun initializeId(): Long {

        takeId()?.let @Synchronized { id ->

            if (id != 0L) @Synchronized {

                return id
            }
        }

        return idProvider().generateNewId()
    }

    protected abstract fun getEntityKind(): Obtain<String>

    private fun idProvider(): EntityIdProvide = EntityIdProvider(getEntityKind())
}
