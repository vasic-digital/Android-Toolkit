package com.redelf.commons.data.access

import com.redelf.commons.management.DataManagement
import com.redelf.commons.obtain.Obtain

abstract class DataAccess<T, M : DataManagement<*>>(

    val managerAccess: Obtain<M>,

    /*
        TODO: Introduce some nice method such as: link(manager: DataManagement<*>)
    */
    val linkedManagers: Obtain<List<Obtain<DataManagement<*>>>>? = null,

) : Obtain<Collection<T?>?>