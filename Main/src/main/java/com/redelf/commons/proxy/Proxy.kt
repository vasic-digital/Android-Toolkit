package com.redelf.commons.proxy

import android.content.Context

abstract class Proxy(var address: String, var port: Int) {

    abstract fun isAlive(ctx: Context): Boolean

    abstract fun getSpeed(ctx: Context): Long

    abstract fun getQuality(): Long
}