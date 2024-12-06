package com.redelf.commons.activity

interface ActivityActiveStateListener {

    fun onActivityStateChanged(activity: BaseActivity, active: Boolean)
}