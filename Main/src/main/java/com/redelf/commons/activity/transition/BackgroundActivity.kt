package com.redelf.commons.activity.transition

import android.os.Bundle
import com.redelf.commons.R
import com.redelf.commons.activity.base.BaseActivity

class BackgroundActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_background)
    }
}