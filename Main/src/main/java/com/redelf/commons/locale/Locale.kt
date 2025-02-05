package com.redelf.commons.locale

object Locale {

    fun getLocale(forceLocale: String = Locale.getDefault().language): String {

        return forceLocale
    }
}