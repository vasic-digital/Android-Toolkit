package com.redelf.commons.extensions

import android.text.Html
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.redelf.commons.logging.Console
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

fun TextView.textAsHtml(htmlCode: String) {

    text = Html.fromHtml(htmlCode, Html.FROM_HTML_MODE_COMPACT).trim()
}

fun TextView.textAsHtml(htmlCode: Int) {

    this.textAsHtml(context.getString(htmlCode))
}

fun ImageView.rectImage(imgRes: Int) {

    try {

        Glide.with(this)
            .load(imgRes)
            .into(this)

    } catch (e: Exception) {

        Console.error(e)
    }
}

fun ImageView.rectImage(imgUrl: String) {

    try {

        Glide.with(this)
            .load(imgUrl)
            .into(this)

    } catch (e: Exception) {

        Console.error(e)
    }
}

fun ImageView.circularImage(imgUrl: String, cornerRadius: Int) {

    try {

        Glide.with(this)
            .load(imgUrl)
            .apply(

                bitmapTransform(

                    RoundedCornersTransformation(

                        cornerRadius, 0,
                        RoundedCornersTransformation.CornerType.ALL
                    )
                )
            )
            .into(this)

    } catch (e: Exception) {

        Console.error(e)
    }
}