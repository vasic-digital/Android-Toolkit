package com.redelf.commons.direction

import com.redelf.commons.obtain.Obtain

interface PushWithConditionCheck<K> {

    fun <T> push(

        key: K,
        what: T,

        check: Obtain<Boolean> = object : Obtain<Boolean> {

            override fun obtain(): Boolean = true
        }

    ): Boolean
}