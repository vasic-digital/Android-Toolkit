package com.redelf.commons.settings

import com.redelf.commons.persistance.base.get.GetBoolean
import com.redelf.commons.persistance.base.get.GetLong
import com.redelf.commons.persistance.base.get.GetString
import com.redelf.commons.persistance.base.put.PutBoolean
import com.redelf.commons.persistance.base.put.PutLong
import com.redelf.commons.persistance.base.put.PutString

interface SettingsManagement :

    GetLong,
    PutLong,
    GetString,
    PutString,
    GetBoolean,
    PutBoolean

{

    fun <T> put(key: String, value: T): Boolean

    fun <T> get(key: String, defaultValue: T): T
}
