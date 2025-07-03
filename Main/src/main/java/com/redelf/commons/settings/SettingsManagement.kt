package com.redelf.commons.settings

import com.redelf.commons.obtain.OnObtain
import com.redelf.commons.persistance.base.get.GetBoolean
import com.redelf.commons.persistance.base.get.GetBooleanAsync
import com.redelf.commons.persistance.base.get.GetLong
import com.redelf.commons.persistance.base.get.GetLongAsync
import com.redelf.commons.persistance.base.get.GetString
import com.redelf.commons.persistance.base.get.GetStringAsync
import com.redelf.commons.persistance.base.put.PutBoolean
import com.redelf.commons.persistance.base.put.PutBooleanAsync
import com.redelf.commons.persistance.base.put.PutLong
import com.redelf.commons.persistance.base.put.PutLongAsync
import com.redelf.commons.persistance.base.put.PutString
import com.redelf.commons.persistance.base.put.PutStringAsync

interface SettingsManagement :

    GetLongAsync,
    PutLongAsync,
    GetStringAsync,
    PutStringAsync,
    GetBooleanAsync,
    PutBooleanAsync

{

    fun <T> put(key: String, value: T, callback: OnObtain<Boolean>)

    fun <T> get(key: String, defaultValue: T, callback: OnObtain<T>)
}
