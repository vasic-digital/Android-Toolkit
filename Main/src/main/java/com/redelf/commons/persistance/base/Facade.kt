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


package com.redelf.commons.persistance.base

import com.redelf.commons.lifecycle.TerminationSynchronizedParametrized
import com.redelf.commons.lifecycle.initialization.InitializationWithContext
import com.redelf.commons.lifecycle.shutdown.ShutdownSynchronized
import com.redelf.commons.obtain.OnObtain
import java.lang.reflect.Type

interface Facade : ShutdownSynchronized, TerminationSynchronizedParametrized, InitializationWithContext {
    fun <T> put(key: String?, value: T): Boolean

    fun <T> get(key: String?, callback: OnObtain<T?>)

    fun <T> get(key: String?, defaultValue: T, callback: OnObtain<T?>)

    fun getByType(key: String?, type: Type, callback: OnObtain<Any?>)

    fun getByClass(key: String?, clazz: Class<*>, callback: OnObtain<Any?>)

    fun count(): Long

    fun deleteAll(): Boolean

    fun delete(key: String?): Boolean

    fun contains(key: String?, callback: OnObtain<Boolean?>)
}
