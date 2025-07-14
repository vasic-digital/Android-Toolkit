package com.redelf.commons.data.access

import com.redelf.commons.management.DataManagement
import com.redelf.commons.obtain.Obtain

abstract class DataAccess<T, M : DataManagement<*>>(

    val managerAccess: Obtain<M>

) : Obtain<Collection<T?>?>