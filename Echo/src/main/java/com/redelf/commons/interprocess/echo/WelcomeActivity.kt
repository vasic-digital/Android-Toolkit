package com.redelf.commons.interprocess.echo

import com.redelf.commons.activity.BaseActivity
import com.redelf.commons.extensions.exec
import org.junit.Assert

class WelcomeActivity : BaseActivity() {

    override fun onPostResume() {
        super.onPostResume()

        exec {

            val worker = EchoService.obtain()

            Assert.assertNotNull(worker)
            Assert.assertTrue(worker.isReady())

            worker.sendMessage(EchoService.ACTION_HELLO)
        }
    }
}