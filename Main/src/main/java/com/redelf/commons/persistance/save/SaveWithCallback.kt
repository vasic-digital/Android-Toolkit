package com.redelf.commons.persistance.save

interface SaveWithCallback<T> {

    fun save(data: T, callback: SaveCallback<T>)
}