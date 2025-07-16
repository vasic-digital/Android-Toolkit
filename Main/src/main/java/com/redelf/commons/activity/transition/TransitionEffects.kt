package com.redelf.commons.activity.transition

import java.lang.annotation.Inherited

@Inherited
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
annotation class TransitionEffects(

    val enter: String,
    val exit: String
)