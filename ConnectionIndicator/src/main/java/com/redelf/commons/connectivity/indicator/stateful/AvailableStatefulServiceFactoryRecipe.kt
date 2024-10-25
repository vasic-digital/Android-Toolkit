package com.redelf.commons.connectivity.indicator.stateful

import com.redelf.commons.obtain.Obtain

data class AvailableStatefulServiceFactoryRecipe(

    val clazz: Class<*>, val obtain: Obtain<AvailableStatefulService>
)