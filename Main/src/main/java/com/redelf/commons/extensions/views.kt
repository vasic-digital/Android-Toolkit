package com.redelf.commons.extensions

import android.text.Html
import android.widget.TextView

fun TextView.textAsHtml(htmlCode: String) {

    text = Html.fromHtml(htmlCode, Html.FROM_HTML_MODE_COMPACT)
}

fun TextView.textAsHtml(htmlCode: Int) {

    text = Html.fromHtml(context.getString(htmlCode), Html.FROM_HTML_MODE_COMPACT)
}