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


package com.redelf.commons.messaging.firebase

import android.content.Context
import com.redelf.commons.net.connectivity.ConnectionBlockingBehavior
import com.redelf.commons.net.connectivity.Connectivity
import com.redelf.commons.net.connectivity.ConnectivityStateChanges
import com.redelf.commons.net.connectivity.StatefulBasicConnectionHandler
import com.redelf.commons.obtain.Obtain

class FcmConnectivityHandler private constructor(

    defaultConnectionBlockState: ConnectionBlockingBehavior = ConnectionBlockingBehavior.DO_NOT_BLOCK

) : StatefulBasicConnectionHandler(defaultConnectionBlockState) {

    companion object : Obtain<FcmConnectivityHandler> {

        private var instance: FcmConnectivityHandler? = null

        
        override fun obtain(): FcmConnectivityHandler {

            return obtain(

                ConnectionBlockingBehavior.DO_NOT_BLOCK
            )
        }

        
        fun obtain(

            defaultConnectionBlockState: ConnectionBlockingBehavior

        ): FcmConnectivityHandler {

            instance?.let {

                return it
            }

            val handler = FcmConnectivityHandler(defaultConnectionBlockState)
            instance = handler
            return handler
        }
    }

    override fun register(subscriber: ConnectivityStateChanges) {

        FcmService.register(subscriber)
    }

    override fun isRegistered(subscriber: ConnectivityStateChanges): Boolean {

        return FcmService.isRegistered(subscriber)
    }

    override fun unregister(subscriber: ConnectivityStateChanges) {

        FcmService.unregister(subscriber)
    }

    override fun requireNetworkAvailable(ctx: Context): Boolean {

        return Connectivity().requireNetworkAvailable(ctx)
    }
}