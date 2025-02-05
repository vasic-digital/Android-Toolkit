package com.redelf.commons.data.list

interface ListDataSource<T> {

    fun getList(): List<T>
}