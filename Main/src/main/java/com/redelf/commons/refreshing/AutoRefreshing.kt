package com.redelf.commons.refreshing

interface AutoRefreshing : Refreshing {

    fun startRefreshing()

    fun stopRefreshing()
}