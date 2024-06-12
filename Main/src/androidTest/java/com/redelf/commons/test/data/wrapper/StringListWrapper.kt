package com.redelf.commons.test.data.wrapper

import java.util.concurrent.CopyOnWriteArrayList

class StringListWrapper(list: CopyOnWriteArrayList<String>) : TypeListWrapper<String>(list) {

    constructor() : this(CopyOnWriteArrayList())

    override fun getClazz(): Class<StringListWrapper> {

        return StringListWrapper::class.java
    }
}