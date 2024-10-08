package com.redelf.commons.extensions

import android.app.Activity
import android.graphics.Color
import android.text.Html
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.redelf.commons.logging.Console
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

data class ColoredWord(

    @SerializedName("text")
    @JsonProperty("text")
    val text: String,

    @SerializedName("color")
    @JsonProperty("color")
    val color: String? = null
)

data class ColoredText(

    @SerializedName("words")
    @JsonProperty("words")
    val words: List<ColoredWord>,

    @SerializedName("defaultColor")
    @JsonProperty("defaultColor")
    val defaultColor: String = "#000000"
)

fun TextView.coloredText(coloredText: ColoredText) {

    val raw = StringBuilder("")

    coloredText.words.forEach {

        raw.append(it.text).append(" ")
    }

    val spannableString = SpannableStringBuilder("")

    coloredText.words.forEach {

        val word = it.text
        val rawColor = it.color

        word.color(

            color = rawColor ?: coloredText.defaultColor,
            words = word.wrapToList()

        ).let { spannable ->

            spannableString.append(spannable).append(SpannableString(" "))
        }
    }

    text = spannableString
}

fun TextView.textAsHtml(htmlCode: String) {

    text = Html.fromHtml(htmlCode, Html.FROM_HTML_MODE_COMPACT).trim()
}

fun TextView.textAsHtml(htmlCode: Int) {

    this.textAsHtml(context.getString(htmlCode))
}

fun ImageView.rectImage(imgRes: Int) {

    onUiThread {

        val ctx = this.context

        if (ctx is Activity) {

            if (ctx.isFinishing || ctx.isDestroyed) {

                return@onUiThread
            }
        }

        try {

            Glide.with(this)
                .load(imgRes)
                .into(this)

        } catch (e: Exception) {

            Console.error(e)
        }
    }
}

fun ImageView.rectImage(imgUrl: String) {

    onUiThread {

        val ctx = this.context

        if (ctx is Activity) {

            if (ctx.isFinishing || ctx.isDestroyed) {

                return@onUiThread
            }
        }

        try {

            Glide.with(this)
                .load(imgUrl)
                .into(this)

        } catch (e: Exception) {

            Console.error(e)
        }
    }
}

fun ImageView.circularImage(imgUrl: String, cornerRadius: Int) {

    onUiThread {

        try {

            val ctx = this.context

            if (ctx is Activity) {

                if (ctx.isFinishing || ctx.isDestroyed) {

                    return@onUiThread
                }
            }

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
}