package com.redelf.commons.interprocess.echo

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.redelf.commons.activity.BaseActivity
import com.redelf.commons.interprocess.InterprocessService
import com.redelf.commons.logging.Console

class WelcomeActivity : BaseActivity() {

    private val tag = "IPC :: Test screen ::"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_welcome)

        findViewById<View>(R.id.self_test).setOnClickListener {

            Console.log("$tag Button clicked")

            val hello = EchoInterprocessProcessor.ACTION_HELLO
            InterprocessService.send(function = hello)

            Console.log("$tag Sent echo intent")
        }
    }
}