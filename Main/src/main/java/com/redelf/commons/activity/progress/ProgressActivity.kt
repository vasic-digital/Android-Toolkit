package com.redelf.commons.activity.progress

interface ProgressActivity {

    fun showProgress(from: String)

    fun hideProgress(from: String)

    fun toggleProgress(show: Boolean, from: String)
}