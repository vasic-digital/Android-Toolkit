package com.redelf.commons.test.data.wrapper

import com.redelf.commons.test.data.SampleData3
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ObjectMapWrapper(map: ConcurrentHashMap<UUID, SampleData3>) :

    TypeMapWrapper<UUID, SampleData3>(map)
{

    constructor() : this(ConcurrentHashMap())

    override fun getClazz(): Class<ObjectMapWrapper> {

        return ObjectMapWrapper::class.java
    }
}