package com.redelf.commons.activity.transition

import android.os.Bundle
import com.redelf.commons.R
import com.redelf.commons.activity.base.BaseActivity
import com.redelf.commons.logging.Console

class BackgroundActivity : BaseActivity() {

    private val tag = "Background activity ::"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_background)

        Console.log("$tag onCreate")
    }

    override fun onResume() {
        super.onResume()

        Console.log("$tag onResume")
    }

    override fun onPause() {
        super.onPause()

        Console.log("$tag onPause")
    }

    override fun onDestroy() {
        super.onDestroy()

        Console.log("$tag onDestroy")
    }
}