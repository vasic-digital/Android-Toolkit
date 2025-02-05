package com.redelf.access.installation

interface Installation {

    fun install()

    fun checkInstalled(callback: InstallationCheckCallback)
}