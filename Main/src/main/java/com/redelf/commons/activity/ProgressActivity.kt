package com.redelf.commons.activity

interface ProgressActivity {

    fun showProgress()

    fun hideProgress()

    fun toggleProgress(show: Boolean)
}