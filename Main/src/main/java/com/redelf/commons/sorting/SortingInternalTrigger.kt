package com.redelf.commons.sorting

import androidx.annotation.NonNull

interface SortingInternalTrigger {

    fun sort(@NonNull parameters: SortingParameters) : Boolean
}