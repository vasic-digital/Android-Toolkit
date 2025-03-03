package com.redelf.commons.rendering

interface Renderable<out T> {

    fun render() : T
}