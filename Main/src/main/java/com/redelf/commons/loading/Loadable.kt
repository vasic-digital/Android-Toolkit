package com.redelf.commons.loading

interface Loadable {

    fun load()

    fun isLoaded(): Boolean
}

fun Loadable.withLoadable(

    ifLoaded: () -> Unit,
    ifNotLoaded: () -> Unit

) {

    if (isLoaded()) {

        ifLoaded()

    } else {

        ifNotLoaded()
    }
}