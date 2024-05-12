package com.redelf.commons.management

import java.util.concurrent.atomic.AtomicBoolean

abstract class LazyDataManagement<T> : DataManagement<T>() {

    protected open val lazySaving = false

    private val saved = AtomicBoolean()

    override fun pushData(data: T) {

        if (lazySaving) {

            saved.set(false)

        } else {

            super.pushData(data)
        }
    }

    override fun onDataPushed(success: Boolean?, err: Throwable?) {
        super.onDataPushed(success, err)

        success?.let {

            if (it) {

                saved.set(true)
            }
        }
    }
}