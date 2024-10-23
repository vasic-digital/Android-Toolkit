package com.redelf.commons.connectivity.indicator.view

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout

class ConnectivityIndicator : RelativeLayout {

    // TODO: Implement the custom view here - #Availability

    constructor(ctx: Context) : super(ctx)

    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)

    constructor(

        ctx: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int

    ) : super(ctx, attrs, defStyleAttr)

    constructor(

        ctx: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int

    ) : super(ctx, attrs, defStyleAttr, defStyleRes)
}