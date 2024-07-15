package com.redelf.commons.proxy

import android.content.Context
import com.redelf.commons.timeout.Timeout

abstract class Proxy(var address: String, var port: Int) : Comparable<Proxy>, Timeout {

    abstract fun isAlive(ctx: Context): Boolean

    abstract fun getSpeed(ctx: Context): Long

    abstract fun getQuality(): Long
}