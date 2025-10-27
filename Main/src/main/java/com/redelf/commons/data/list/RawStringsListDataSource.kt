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


package com.redelf.commons.data.list

import android.content.Context
import android.content.res.Resources.NotFoundException
import com.redelf.commons.extensions.isNotEmpty
import com.redelf.commons.extensions.readRawTextFile
import com.redelf.commons.extensions.recordException
import java.io.IOException

class RawStringsListDataSource(

    private val ctx: Context,
    private val resId: Int,
    private val throwOnError: Boolean = false

) : ListDataSource<String> {

    @Throws(NotFoundException::class, IOException::class, IllegalStateException::class)
    override fun getList(): List<String> {

        try {

            val raw = ctx.readRawTextFile(resId)

            if (isNotEmpty(raw)) {

                return raw.split("\n")
            }

        } catch (e: Throwable) {

            if (throwOnError) {

                throw e
            }

            recordException(e)
        }

        return emptyList()
    }
}