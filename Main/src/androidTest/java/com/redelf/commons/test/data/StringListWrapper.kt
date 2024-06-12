package com.redelf.commons.test.data

import com.google.gson.reflect.TypeToken
import com.redelf.commons.logging.Timber
import com.redelf.commons.model.Wrapper
import com.redelf.commons.partition.Partitional
import org.junit.Assert
import java.lang.reflect.Type
import java.util.concurrent.CopyOnWriteArrayList

class StringListWrapper(list: CopyOnWriteArrayList<String>) : TypeListWrapper<String>(list) {

    constructor() : this(CopyOnWriteArrayList())

    override fun getClazz(): Class<StringListWrapper> {

        return StringListWrapper::class.java
    }
}