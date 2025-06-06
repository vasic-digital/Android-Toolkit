package com.redelf.commons.activity

import android.net.Uri
import android.os.Bundle
import com.redelf.commons.logging.Console

abstract class DeepLinkActivity : BaseActivity() {

    protected open val tag = "Deep linking :: Activity ::"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Console.log("$tag START")

        val data: Uri? = intent?.data

        data?.let {

            val controller = it.host
            val parameter = it.lastPathSegment

            onDeepLink(controller, parameter)
        }
    }

    protected open fun onDeepLink(controller: String?, parameter: String? = null) {

        Console.log("$tag RECEIVED :: controller = '$controller', parameter = '$parameter'")

        handleDeepLink(controller, parameter)

        Console.log("$tag END")
    }

    abstract fun handleDeepLink(controller: String?, parameter: String? = null)
}