package com.redelf.commons.sorting

import androidx.annotation.NonNull

interface SortingExternalTrigger<T> {

    fun sort(

        @NonNull what: Collection<T>,
        @NonNull parameters: SortingParameters

    ) : Boolean
}