package com.redelf.commons.data.list

fun interface ListDataSource<T> {

    fun getList(): List<T>
}