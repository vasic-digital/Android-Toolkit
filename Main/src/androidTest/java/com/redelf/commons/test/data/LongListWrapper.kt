package com.redelf.commons.test.data

import java.util.concurrent.CopyOnWriteArrayList

class LongListWrapper(list: CopyOnWriteArrayList<Long>) : TypeListWrapper<Long>(list) {

    constructor() : this(CopyOnWriteArrayList())

    override fun getClazz(): Class<LongListWrapper> {

        return LongListWrapper::class.java
    }
}