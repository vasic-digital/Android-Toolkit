package com.redelf.commons.interprocess.echo

import android.os.Bundle
import android.view.View
import com.redelf.commons.activity.BaseActivity
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.interprocess.Interprocessor
import com.redelf.commons.logging.Console

class WelcomeActivity : BaseActivity() {

    private val tag = "IPC :: Test screen ::"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_welcome)

        findViewById<View>(R.id.self_test).setOnClickListener {

            Console.log("$tag Button clicked")

            val hello = EchoInterprocessProcessor.ACTION_HELLO
            val receiver = BaseApplication.takeContext().packageName
            val sent = Interprocessor.send(receiver = receiver, function = hello)

            if (sent) {

                Console.log("$tag Sent echo intent")
            }
        }
    }
}