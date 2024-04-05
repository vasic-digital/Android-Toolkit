package com.redelf.commons.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import com.redelf.commons.application.BaseApplication
import timber.log.Timber

@SuppressLint("CustomSplashScreen")
abstract class BaseSplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (BaseApplication.TOP_ACTIVITY.isNotEmpty()) {

            val alive = BaseApplication.TOP_ACTIVITY.last()

            val tag = "${BaseApplication.ACTIVITY_LIFECYCLE_TAG} :: Alive ::"

            Timber.v("$tag Activity: ${alive.simpleName}")

            finish()

            val intent = Intent(this, alive)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)

            return
        }
    }
}