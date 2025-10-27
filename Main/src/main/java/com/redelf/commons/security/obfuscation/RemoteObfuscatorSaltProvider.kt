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


package com.redelf.commons.security.obfuscation

import com.redelf.commons.extensions.exec
import com.redelf.commons.net.content.RemoteHttpContentFetcher
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.security.management.SecretsManager

class RemoteObfuscatorSaltProvider(

    private val endpoint: String,
    private val token: String

) : ObfuscatorSaltProvider {

    fun getRemoteData(): String {

        return RemoteHttpContentFetcher(endpoint, token).fetch()
    }

    override fun obtain(callback: OnObtain<ObfuscatorSalt?>) {

        exec {

            SecretsManager.obtain().getObfuscationSalt(this@RemoteObfuscatorSaltProvider, callback)
        }
    }
}
