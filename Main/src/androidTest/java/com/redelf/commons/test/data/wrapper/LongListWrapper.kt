package com.redelf.commons.test.data.wrapper

import java.util.concurrent.CopyOnWriteArrayList

class LongListWrapper(list: CopyOnWriteArrayList<Double>) : TypeListWrapper<Double>(list) {

    constructor() : this(CopyOnWriteArrayList())

    override fun getClazz(): Class<LongListWrapper> {

        return LongListWrapper::class.java
    }
}