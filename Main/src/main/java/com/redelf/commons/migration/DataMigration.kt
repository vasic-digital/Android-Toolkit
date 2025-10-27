/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


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