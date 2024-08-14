package com.redelf.commons.interprocess.echo

import android.content.Intent
import com.redelf.commons.activity.BaseActivity
import com.redelf.commons.extensions.exec
import org.junit.Assert

class WelcomeActivity : BaseActivity() {

    override fun onPostResume() {
        super.onPostResume()

        val intent = Intent(EchoService.ACTION_HELLO)

        applicationContext.sendBroadcast(intent)
    }
}