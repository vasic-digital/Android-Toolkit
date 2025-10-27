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


package com.redelf.commons.interprocess.echo

import android.os.Bundle
import android.view.View
import com.redelf.commons.activity.base.BaseActivity
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.interprocess.Interprocessor
import com.redelf.commons.logging.Console

class WelcomeActivity : BaseActivity() {

    private val tag = "IPC :: Test screen ::"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_welcome)

        findViewById<View>(R.id.self_test).setOnClickListener {

            Console.log("$tag Button clicked")

            val hello = EchoInterprocessProcessor.ACTION_HELLO
            val receiver = BaseApplication.takeContext().packageName
            val sent = Interprocessor.send(receiver = receiver, function = hello)

            if (sent) {

                Console.log("$tag Sent echo intent")
            }
        }
    }
}