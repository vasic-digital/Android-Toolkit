package com.redelf.commons.ui.dialog

import com.redelf.commons.dismissal.Dismissable
import com.redelf.commons.visibility.Showing

interface Dialog : Showing, Dismissable {

    val layout: Int
}