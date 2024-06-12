package com.redelf.commons.test.data

import java.util.concurrent.CopyOnWriteArrayList

class ObjectListWrapper(list: CopyOnWriteArrayList<SampleData3>) : TypeListWrapper<SampleData3>(list) {

    constructor() : this(CopyOnWriteArrayList())

    override fun getClazz(): Class<ObjectListWrapper> {

        return ObjectListWrapper::class.java
    }
}