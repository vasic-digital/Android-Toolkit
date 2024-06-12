package com.redelf.commons.test.data


class LongWrapper(list: Long) : TypeWrapper<Long>(list) {

    constructor() : this(0)

    override fun getClazz(): Class<LongWrapper> {

        return LongWrapper::class.java
    }
}