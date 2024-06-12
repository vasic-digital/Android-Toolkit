package com.redelf.commons.test.data.wrapper

import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type


class BoolWrapper(wrapped: Boolean) : TypeWrapper<Boolean>(wrapped) {

    constructor() : this(false)

    override fun getClazz(): Class<BoolWrapper> {

        return BoolWrapper::class.java
    }

    override fun getPartitionType(number: Int): Type? {

        return object : TypeToken<Boolean?>() {}.type
    }
}