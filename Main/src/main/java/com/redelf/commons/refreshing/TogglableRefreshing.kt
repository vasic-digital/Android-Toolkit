package com.redelf.commons.refreshing

interface TogglableRefreshing : Refreshable {

    fun startRefreshing()

    fun stopRefreshing()

    fun toggleRefreshing()
}