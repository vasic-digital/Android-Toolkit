package com.redelf.commons.interprocess.echo

import android.content.Intent
import com.redelf.commons.activity.BaseActivity
import org.junit.Assert

class WelcomeActivity : BaseActivity() {

    override fun onPostResume() {
        super.onPostResume()

        Assert.assertNotNull(EchoWorker.WORKER)

        EchoWorker.WORKER?.sendMessage(EchoWorker.ACTION_HELLO)
    }
}