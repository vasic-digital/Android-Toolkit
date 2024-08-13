package com.redelf.commons.interprocess.echo

import com.redelf.commons.application.BaseApplication

class EchoApplication : BaseApplication() {

    override val firebaseEnabled = false

    override fun isProduction() = false

    override fun takeSalt() = "echo_salt"
}