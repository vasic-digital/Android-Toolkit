package com.redelf.commons.test.data.wrapper

import java.util.concurrent.CopyOnWriteArrayList

class BoolListWrapper(list: CopyOnWriteArrayList<Boolean>) : TypeListWrapper<Boolean>(list) {

    constructor() : this(CopyOnWriteArrayList())

    override fun getClazz(): Class<BoolListWrapper> {

        return BoolListWrapper::class.java
    }
}