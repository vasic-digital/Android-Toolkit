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


package com.redelf.commons.persistance

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import com.redelf.commons.extensions.exec
import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.persistance.base.Storage

class SharedPreferencesStorage internal constructor(

    private val preferences: SharedPreferences

) : Storage<String?> {

    constructor(context: Context) : this(

        context.getSharedPreferences(

            context.packageName,
            Context.MODE_PRIVATE
        )
    )

    override fun shutdown(): Boolean {

        return true
    }

    override fun terminate(vararg args: Any): Boolean {

        return true
    }

    override fun initialize(ctx: Context) {

        // Ignore
    }

    override fun put(key: String?, value: String?): Boolean {

        if (TextUtils.isEmpty(key)) {

            return false
        }

        return getEditor()!!.putString(key, value.toString()).commit()
    }

    override fun get(key: String?, callback: OnObtain<String?>) {

        exec {

            val result = preferences.getString(key, "")
            callback.onCompleted(result)
        }
    }

    override fun delete(key: String?): Boolean {

        return getEditor()?.remove(key)?.commit() == true
    }

    override fun contains(key: String?, callback: OnObtain<Boolean?>) {

        exec {

            val contains = preferences.contains(key)
            callback.onCompleted(contains)
        }
    }

    override fun deleteAll(): Boolean {

        return getEditor()?.clear()?.commit() == true
    }

    override fun count(): Long {

        return preferences.all?.size?.toLong() ?: 0L
    }

    private fun getEditor(): SharedPreferences.Editor? {

        return preferences.edit()
    }
}
