package com.redelf.commons.extensions

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

fun <T> LiveData<T>.asFlow(): Flow<T> = callbackFlow {

    val observer = Observer<T> { value ->

        trySend(value)
    }

    observeForever(observer)
    awaitClose {

        removeObserver(observer)
    }
}