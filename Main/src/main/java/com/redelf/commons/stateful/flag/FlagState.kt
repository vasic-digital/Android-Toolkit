package com.redelf.commons.stateful.flag

import com.redelf.commons.callback.CallbackOperation
import com.redelf.commons.callback.Callbacks
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.registration.Registration
import com.redelf.commons.value.Get
import com.redelf.commons.value.Set
import java.util.concurrent.atomic.AtomicBoolean

class FlagState(context: String) :

    Get<Boolean>,
    Set<Boolean>,
    Registration<OnObtain<Boolean>>

{

    private val value = AtomicBoolean()
    private val callbacks = Callbacks<OnObtain<Boolean>>(context)

    override fun isRegistered(subscriber: OnObtain<Boolean>): Boolean {

        return callbacks.isRegistered(subscriber)
    }

    override fun register(subscriber: OnObtain<Boolean>) {

        if (callbacks.isRegistered(subscriber)) {

            return
        }

        callbacks.register(subscriber)

        subscriber.onCompleted(value.get())
    }

    override fun unregister(subscriber: OnObtain<Boolean>) {

        if (!callbacks.isRegistered(subscriber)) {

            return
        }

        callbacks.unregister(subscriber)
    }

    override fun get(): Boolean {

        return value.get()
    }

    override fun set(value: Boolean) {

        val oldValue = this.value.getAndSet(value)

        if (oldValue != value) {

            callbacks.doOnAll(

                object : CallbackOperation<OnObtain<Boolean>> {

                    override fun perform(callback: OnObtain<Boolean>) {

                        callback.onCompleted(value)
                    }
                }, "set"
            )
        }
    }
}