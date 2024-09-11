package com.redelf.commons.activity

interface ProgressActivity {

    fun showProgress(from: String)

    fun hideProgress(from: String)

    fun toggleProgress(show: Boolean, from: String)
}