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


package com.redelf.commons.connectivity.indicator.implementation

import com.redelf.commons.connectivity.indicator.connection.ConnectionAvailableService
import com.redelf.commons.connectivity.indicator.stateful.AvailableStatefulService
import com.redelf.commons.creation.instantiation.SingleInstance
import com.redelf.commons.creation.instantiation.SingleInstantiated
import com.redelf.commons.logging.Console
import com.redelf.commons.messaging.firebase.FcmConnectivityHandler
import com.redelf.commons.messaging.firebase.FcmService
import com.redelf.commons.net.connectivity.Reconnect
import com.redelf.commons.net.connectivity.StatefulBasicConnectionHandler
import com.redelf.commons.obtain.Obtain
import com.redelf.commons.obtain.Obtainer

class FCMConnectionAvailabilityService private constructor(origin: String) :

    ConnectionAvailabilityService(

        handlerObtain = object : Obtain<StatefulBasicConnectionHandler> {

            override fun obtain(): StatefulBasicConnectionHandler {

                return FcmConnectivityHandler.obtain()
            }
        },

        origin = origin,

    ), Reconnect, SingleInstantiated
{

    companion object :

        SingleInstance<ConnectionAvailableService>(),
        Obtainer<AvailableStatefulService>

    {

        @Throws(IllegalArgumentException::class)
        override fun instantiate(vararg params: Any): ConnectionAvailableService {

            if (params.isEmpty() || params[0] !is String) {

                throw IllegalArgumentException("Origin parameter must be a String")
            }

            return FCMConnectionAvailabilityService(params[0] as String)
        }

        @Throws(IllegalArgumentException::class)
        override fun getObtainer(vararg params: Any): Obtain<AvailableStatefulService> {

            if (params.isEmpty() || params[0] !is String) {

                throw IllegalArgumentException("Origin parameter must be a String")
            }

            return object : Obtain<AvailableStatefulService> {

                override fun obtain() = instantiate(params[0] as String)
            }
        }
    }

    override fun getWho() = "Push notifications"

    override fun identifier() = "Connectivity :: Availability :: FCM :: ${hashCode()}"

    override fun reconnect() {

        Console.log("${tag()} Reconnecting...")

        FcmService.reconnect()
    }
}