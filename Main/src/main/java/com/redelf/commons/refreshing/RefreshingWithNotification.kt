package com.redelf.commons.refreshing

interface RefreshingWithNotification : Refreshable {

    fun refresh(notify: Boolean): Boolean
}