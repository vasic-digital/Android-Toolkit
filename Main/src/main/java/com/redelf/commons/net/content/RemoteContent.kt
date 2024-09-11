package com.redelf.commons.net.content

interface RemoteContent<out T> {

    fun fetch(): T?
}