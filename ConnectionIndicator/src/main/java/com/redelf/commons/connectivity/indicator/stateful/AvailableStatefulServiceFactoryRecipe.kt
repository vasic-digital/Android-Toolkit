package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.obtain.suspendable.Obtain

data class AvailableStatefulServiceFactoryRecipe(

    val clazz: Class<*>,
    val obtain: Obtain<AvailableStatefulService>,
    val dependencies: List<Obtain<AvailableStatefulService>> = emptyList()
)