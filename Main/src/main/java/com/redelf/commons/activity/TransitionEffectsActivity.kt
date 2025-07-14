package com.redelf.commons.activity

import android.content.Intent
import com.redelf.commons.extensions.finishWithTransition
import com.redelf.commons.extensions.startActivityWithTransition

abstract class TransitionEffectsActivity : BaseActivity() {

    override fun startActivity(intent: Intent?) {

        intent?.let {

            startActivityWithTransition(it)
        }
    }

    override fun finish() {

        finishWithTransition()
    }
}