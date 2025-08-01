package com.redelf.commons.activity.fragment

import com.redelf.commons.activity.transition.TransitionEffectsActivity

interface ActivityPresentable {

    fun showInActivity(activity: Class<*>, context: TransitionEffectsActivity): Boolean
}