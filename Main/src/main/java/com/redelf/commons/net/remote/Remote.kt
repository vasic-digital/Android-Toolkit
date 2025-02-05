package com.redelf.commons.net.remote

import android.content.Context

interface Remote {

    fun ping(): Boolean

    fun isAlive(ctx: Context): Boolean

    fun getSpeed(ctx: Context): Long

    fun getQuality(): Long
}