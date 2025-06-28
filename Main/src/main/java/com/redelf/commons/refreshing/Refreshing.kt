package com.redelf.commons.refreshing

interface Refreshing : Refreshable {

    fun refresh(): Boolean
}