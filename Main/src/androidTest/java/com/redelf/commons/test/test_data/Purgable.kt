package com.redelf.commons.test.test_data

import com.redelf.commons.data.model.identifiable.Identifiable
import com.redelf.commons.destruction.delete.DeletionCheck

class Purgable<T>(

    private val value: T,
    private val deleted: Boolean = false

) : Identifiable<T>(), DeletionCheck {

    override fun isDeleted() = deleted

    override fun hasValidId(): Boolean {

        return true
    }

    override fun initializeId(): T {

        return value
    }

    fun takeData() = value
}