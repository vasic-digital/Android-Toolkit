package com.redelf.commons.activity

abstract class TransitionEffectsActivity : BaseActivity() {

    protected open val inEffect: Int = 0
    protected open val outEffect: Int = 0
}