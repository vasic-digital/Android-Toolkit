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


package com.redelf.commons.creation.instantiation

import com.redelf.commons.obtain.ObtainParametrized

abstract class SingleInstanceParametrized<T> :

    SingleInstance<T>(),
    ObtainParametrized<T, Any>

{

    @Throws(InstantiationException::class)
    override fun obtain(): T {

        throw InstantiationException("This method should not be used without parameters")
    }

    @Throws(IllegalArgumentException::class)
    override fun obtain(param: Any): T {

        if (instance == null) {

            instance = instantiate(param)
        }

        instance?.let {

            if (it !is SingleInstantiated) {

                val msg = "${it::class.simpleName} " +
                        "does not implement ${SingleInstantiated::class.simpleName} " +
                        "interface"

                throw InstantiationException(msg)
            }

            return it
        }

        throw InstantiationException("Object is null")
    }
}