package com.redelf.commons.activity.stateful

interface ActivityActiveStateListener {

    fun onDestruction(activity: StatefulActivity)

    fun onActivityStateChanged(activity: StatefulActivity, active: Boolean)
}