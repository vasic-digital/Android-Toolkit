package com.redelf.commons.interprocess.echo

import android.content.Intent
import com.redelf.commons.activity.BaseActivity

class WelcomeActivity : BaseActivity() {

    override fun onPostResume() {
        super.onPostResume()

        val intent = Intent(EchoInterprocessProcessor.ACTION_HELLO)

        applicationContext.sendBroadcast(intent)
    }
}