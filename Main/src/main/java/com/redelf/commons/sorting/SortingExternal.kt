package com.redelf.commons.sorting

import androidx.annotation.NonNull
import com.redelf.commons.obtain.OnObtain

interface SortingExternal<T> {

    fun sort(

        @NonNull what: Collection<T>,
        @NonNull parameters: SortingParameters,
        @NonNull callback: OnObtain<Collection<T>>
    )
}