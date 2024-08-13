package com.redelf.commons.interprocess.echo

import com.redelf.commons.activity.BaseActivity
import com.redelf.commons.extensions.exec
import org.junit.Assert

class WelcomeActivity : BaseActivity() {

    override fun onPostResume() {
        super.onPostResume()

        exec {

            val echo = EchoService.obtain()

            Assert.assertNotNull(echo)
            Assert.assertTrue(echo.isReady())

            echo.sendMessage(EchoService.ACTION_HELLO)
        }
    }
}