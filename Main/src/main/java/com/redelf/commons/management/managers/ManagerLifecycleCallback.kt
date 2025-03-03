package com.redelf.commons.management.managers

import com.redelf.commons.lifecycle.LifecycleCallback
import com.redelf.commons.persistance.EncryptedPersistence

abstract class ManagerLifecycleCallback : LifecycleCallback<EncryptedPersistence> {

    override fun onShutdown(success: Boolean, vararg args: EncryptedPersistence) {

        // Ignore
    }
}