package com.redelf.commons.firebase

import android.annotation.SuppressLint
import com.redelf.commons.context.ContextualManager

@SuppressLint("StaticFieldLeak")
object FirebaseConfigurationManager : ContextualManager<FirebaseRemoteConfiguration>() {

    override val storageKey = "remote_configuration"
}