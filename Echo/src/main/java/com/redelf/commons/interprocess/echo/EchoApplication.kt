package com.redelf.commons.interprocess.echo

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.interprocess.InterprocessApplication
import com.redelf.commons.interprocess.InterprocessProcessor

class EchoApplication : InterprocessApplication() {

    override val firebaseEnabled = false
    override val interprocessPermission = getString(R.string.interprocess_permission)

    override fun isProduction() = false

    override fun takeSalt() = "echo_salt"

    override fun getProcessors(): List<InterprocessProcessor> {

        val echoProcessor = EchoInterprocessProcessor(applicationContext)

        return listOf(echoProcessor)
    }
}