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


package com.redelf.commons.activity

import android.net.Uri
import android.os.Bundle
import com.redelf.commons.activity.base.BaseActivity
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Console

abstract class DeepLinkActivity : BaseActivity() {

    protected open val tag = "Deep linking :: Activity ::"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (BaseApplication.takeContext().isDeepLinkingDisabled()) {

            Console.warning("$tag Deep linking is disabled")
            return
        }

        Console.log("$tag START")

        val data: Uri? = intent?.data

        data?.let {

            val controller = it.host
            val parameter = it.lastPathSegment

            onDeepLink(controller, parameter)
        }
    }

    protected open fun onDeepLink(controller: String?, parameter: String? = null) {

        Console.log("$tag RECEIVED :: controller = '$controller', parameter = '$parameter'")

        handleDeepLink(controller, parameter)

        Console.log("$tag END")
    }

    abstract fun handleDeepLink(controller: String?, parameter: String? = null)
}