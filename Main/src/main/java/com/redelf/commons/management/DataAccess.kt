package com.redelf.commons.management

import com.redelf.commons.obtain.Obtain

abstract class DataAccess<T, M : DataManagement<*>>(

    val managerAccess: Obtain<M>

) : Obtain<Collection<T?>?>