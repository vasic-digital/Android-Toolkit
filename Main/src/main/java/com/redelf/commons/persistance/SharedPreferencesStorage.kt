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
