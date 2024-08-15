package com.redelf.commons.interprocess.echo

import android.content.Intent
import com.redelf.commons.activity.BaseActivity
import com.redelf.commons.interprocess.InterprocessService

class WelcomeActivity : BaseActivity() {

    override fun onPostResume() {
        super.onPostResume()

        val hello = EchoInterprocessProcessor.ACTION_HELLO
        InterprocessService.send(function = hello)
    }
}