package com.redelf.commons.locale

import java.util.Locale

object Locale {

    fun getLocale(forceLocale: String = Locale.getDefault().language): String {

        return forceLocale
    }
}