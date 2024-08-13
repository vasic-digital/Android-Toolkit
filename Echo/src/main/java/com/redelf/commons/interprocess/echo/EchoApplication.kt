package com.redelf.commons.interprocess.echo

import android.content.Intent
import com.redelf.commons.application.BaseApplication

class EchoApplication : BaseApplication() {

    override val firebaseEnabled = false

    override fun isProduction() = false

    override fun takeSalt() = "echo_salt"

    override fun onDoCreate() {
        super.onDoCreate()

        val intent = Intent(this, EchoService::class.java)
        applicationContext.startService(intent)
    }
}