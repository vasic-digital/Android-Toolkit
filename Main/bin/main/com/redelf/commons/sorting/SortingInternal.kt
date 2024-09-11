package com.redelf.commons.sorting

import androidx.annotation.NonNull
import com.redelf.commons.obtain.OnObtain

interface SortingInternal<T> {

    fun sort(

        @NonNull parameters: SortingParameters,
        @NonNull callback: OnObtain<Collection<T>>
    )
}