package com.redelf.commons.activity

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class TransitionEffects(

    val enterAnim: Int,
    val exitAnim: Int
)