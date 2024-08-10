package com.redelf.commons.net.cronet

import android.content.Context
import com.redelf.commons.lifecycle.InitializationParametrized
import com.redelf.commons.lifecycle.LifecycleCallback
import java.util.concurrent.atomic.AtomicBoolean

object Cronet : InitializationParametrized<Boolean, Context> {

    private val ready = AtomicBoolean()

    override fun initialize(param: Context, callback: LifecycleCallback<Boolean>) {

        TODO("Not yet implemented")
    }

    override fun isInitialized(): Boolean {

        TODO("Not yet implemented")
    }

    override fun isInitializing(): Boolean {

        TODO("Not yet implemented")
    }

    override fun initializationCompleted(e: Exception?) {

        TODO("Not yet implemented")
    }
}