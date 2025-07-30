package com.redelf.commons.activity.base

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import com.redelf.commons.application.BaseApplication

@SuppressLint("CustomSplashScreen")
abstract class BaseSplashActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        if (BaseApplication.getForegroundActivityCount() > 0) {

            val alive = BaseApplication.takeContext().getTopActivity()

            alive?.let { clazz ->

                finish()

                val intent = Intent(this, clazz)
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(intent)
            }

            return
        }
    }
}