package com.redelf.commons.data.access

import com.redelf.commons.management.DataManagement
import com.redelf.commons.obtain.Obtain

abstract class DataAccess<T, M : DataManagement<*>>(

    val managerAccess: Obtain<M>,

    /*
        NOTE: Future enhancement - introduce a link(manager: DataManagement<*>) method
        to simplify manager linking with a fluent API instead of Obtain wrappers.
    */
    val linkedManagers: Obtain<List<Obtain<DataManagement<*>>>>? = null,

) : Obtain<Collection<T?>?>