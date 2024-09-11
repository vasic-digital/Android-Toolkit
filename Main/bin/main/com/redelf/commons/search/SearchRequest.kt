package com.redelf.commons.search

import com.redelf.commons.rendering.Renderable

abstract class SearchRequest<out T, out R>(val params: T, val what: String) : Renderable<R>