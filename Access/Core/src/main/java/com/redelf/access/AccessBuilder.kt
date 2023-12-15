package com.redelf.access

import java.util.*

class AccessBuilder {

    val methods = PriorityQueue<AccessMethod>()

    @Throws(IllegalArgumentException::class)
    fun addAccessMethod(method: AccessMethod) : AccessBuilder {

        methods.forEach {
            if (it::class == method::class) {

                val name = method::class.simpleName
                throw IllegalArgumentException("Access method already present: $name")
            }
        }
        methods.add(method)
        return this
    }

    fun build() = Access(this)
}