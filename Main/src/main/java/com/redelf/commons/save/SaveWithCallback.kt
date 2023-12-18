package com.redelf.commons.save

interface SaveWithCallback<T> {

    fun save(data: T, callback: SaveCallback<T>)
}