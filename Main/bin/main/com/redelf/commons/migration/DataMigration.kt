package com.redelf.commons.migration

import com.redelf.commons.logging.Console
import com.redelf.commons.obtain.OnObtain

abstract class DataMigration<SOURCE, TARGET>(

    private val dataManagersReadyRequired: Boolean = true

) {

    /*
        TODO: Support multiple migration contained inside the PriorityQueue ordered by the id (version code)
            - Oldest first
            - Executed sequentially
    */
    abstract val id: Long

    abstract fun getSource(callback: OnObtain<SOURCE>)

    abstract fun getTarget(source: SOURCE, callback: OnObtain<TARGET>)


    fun migrate(managersReady: Boolean, callback: OnObtain<Boolean>) {

        if (dataManagersReadyRequired && !managersReady) {

            val e = MigrationNotReadyException()
            callback.onFailure(e)
            return
        }

        val tag = "Migrate :: $id ::"

        Console.log("$tag START")

        val onTarget = object : OnObtain<TARGET> {

            override fun onCompleted(data: TARGET) {

                Console.log("$tag Target obtained: $data")

                apply(data, callback)
            }

            override fun onFailure(error: Throwable) {

                callback.onFailure(error)
            }
        }

        val onSource = object : OnObtain<SOURCE> {

            override fun onCompleted(data: SOURCE) {

                Console.log("$tag Source obtained: $data")

                data?.let {

                    Console.log("$tag Get target")

                    getTarget(data, onTarget)
                }

                if (data == null) {

                    callback.onCompleted(true)
                }
            }

            override fun onFailure(error: Throwable) {

                callback.onFailure(error)
            }
        }

        Console.log("$tag Get source")

        getSource(onSource)
    }

    abstract fun apply(target: TARGET, callback: OnObtain<Boolean>)
}