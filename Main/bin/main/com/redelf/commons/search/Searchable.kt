package com.redelf.commons.search

import com.redelf.commons.obtain.OnObtain

interface Searchable<in P, T, R> {

    fun search(request: SearchRequest<P, R>, callback: OnObtain<SearchResult<T>>)

    fun abortSearch(request: SearchRequest<P, R>)
}