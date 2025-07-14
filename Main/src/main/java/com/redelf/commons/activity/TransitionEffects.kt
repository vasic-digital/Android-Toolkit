package com.redelf.commons.activity

import java.lang.annotation.Inherited

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class TransitionEffects(

    val enterAnim: Int,
    val exitAnim: Int
)