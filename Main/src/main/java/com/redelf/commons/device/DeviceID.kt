package com.redelf.commons.device

import android.annotation.SuppressLint
import android.content.Context
import android.provider.Settings
import com.redelf.commons.obtain.ObtainParametrized

object DeviceID : ObtainParametrized<String, Context> {

    @SuppressLint("HardwareIds")
    override fun obtain(param: Context): String {

        return Settings.Secure.getString(

            param.contentResolver, Settings.Secure.ANDROID_ID
        )
    }
}