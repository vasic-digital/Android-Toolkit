package com.redelf.commons.activity

interface ActivityActiveStateListener {

    fun onDestruction(activity: StatefulActivity)

    fun onActivityStateChanged(activity: StatefulActivity, active: Boolean)
}