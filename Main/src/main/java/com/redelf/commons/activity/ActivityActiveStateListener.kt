package com.redelf.commons.activity

interface ActivityActiveStateListener {

    fun onActivityStateChanged(activity: StatefulActivity, active: Boolean)
}