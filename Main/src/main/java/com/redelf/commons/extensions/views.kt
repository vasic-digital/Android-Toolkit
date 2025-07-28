package com.redelf.commons.extensions

import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Activity
import android.text.Html
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions.bitmapTransform
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.google.gson.annotations.SerializedName
import com.google.gson.internal.LinkedTreeMap
import com.redelf.commons.logging.Console
import jp.wasabeef.glide.transformations.RoundedCornersTransformation
import java.util.concurrent.ConcurrentHashMap

data class ColoredWord @JsonCreator constructor(

    @SerializedName("text")
    @JsonProperty("text")
    val text: String,

    @SerializedName("color")
    @JsonProperty("color")
    val color: String? = null

) {

    @Throws(ClassCastException::class)
    constructor(treeMap: LinkedTreeMap<String, Any>) : this(

        text = treeMap["text"].toString(),
        color = treeMap["color"].toString()
    )
}

data class ColoredText @JsonCreator constructor(

    @SerializedName("words")
    @JsonProperty("words")
    val words: List<ColoredWord>,

    @SerializedName("defaultColor")
    @JsonProperty("defaultColor")
    val defaultColor: String = "#000000"

) {

    companion object {

        fun convert (what: ArrayList<LinkedTreeMap<String, Any>>): List<ColoredWord> {

            val items = mutableListOf<ColoredWord>()

            what.forEach {

                val item = ColoredWord(it)

                items.add(item)
            }

            return items
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Throws(ClassCastException::class)
    constructor(treeMap: LinkedTreeMap<String, Any>) : this(

        defaultColor = treeMap["defaultColor"].toString(),
        words = convert(treeMap["words"] as ArrayList<LinkedTreeMap<String, Any>>),
    )
}

fun TextView.coloredText(coloredText: ColoredText) {

    val raw = StringBuilder("")

    coloredText.words.forEach {

        val current = it.text
        raw.append(current).append(" ")
    }

    val spannableString = SpannableStringBuilder("")

    coloredText.words.forEachIndexed { index, it ->

        val word = it.text
        val rawColor = it.color

        word.color(

            color = rawColor ?: coloredText.defaultColor,
            words = word.wrapToList()

        ).let { spannable ->

            spannableString.append(spannable)

            if (index < coloredText.words.lastIndex && word != ".") {

                spannableString.append(SpannableString(" "))
            }
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

        } catch (e: Throwable) {

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

        } catch (e: Throwable) {

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

        } catch (e: Throwable) {

            Console.error(e)
        }
    }
}

private val refreshingRecyclerViews = ConcurrentHashMap<Int, Boolean>()

@SuppressLint("NotifyDataSetChanged")
fun RecyclerView.notifyDatasetChangedWithFade(

    from: String,
    duration: Long = 200L

) {

    val thisOne = hashCode()

    if (refreshingRecyclerViews[thisOne] == true) {

        Console.log(

            "Notify dataset changes :: $thisOne :: ${adapter?.hashCode()} :: SKIPPED :: From='$from'"
        )

        return
    }

    refreshingRecyclerViews[thisOne] = true

    Console.log(

        "Notify dataset changes :: $thisOne :: ${adapter?.hashCode()} :: From='$from'"
    )

    // Fade out existing items
    for (i in 0 until childCount) {

        getChildAt(i)
            ?.animate()
            ?.setDuration(duration)
            ?.setListener(

                object : Animator.AnimatorListener {

                    override fun onAnimationCancel(animation: Animator) = Unit

                    override fun onAnimationEnd(animation: Animator) {

                        adapter?.notifyItemChanged(i)
                    }

                    override fun onAnimationRepeat(animation: Animator) = Unit

                    override fun onAnimationStart(animation: Animator) = Unit
                }
            )
            ?.start()
    }

    // After delay, fade in new items
    postDelayed({

        for (i in 0 until childCount) {

            getChildAt(i)?.apply {

                animate().setDuration(duration).start()
            }
        }

        refreshingRecyclerViews[thisOne] = false

    }, duration)
}