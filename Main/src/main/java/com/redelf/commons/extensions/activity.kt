package com.redelf.commons.extensions

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.View
import android.view.WindowInsets
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.DialogFragment
import com.redelf.commons.activity.TransitionEffects
import com.redelf.commons.logging.Console

fun Activity.fitInsideSystemBoundaries() {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {

        window.decorView.setOnApplyWindowInsetsListener { view, insets ->

            val systemBarsInsets = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(0, systemBarsInsets.top, 0, systemBarsInsets.bottom)
            insets
        }
    }
}

fun Activity.getSystemBarsInsets(onInsetsChanged: (top: Int, bottom: Int) -> Unit) {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {

        val rootView: View = window.decorView.findViewById(android.R.id.content)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->

            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            onInsetsChanged(systemBarsInsets.top, systemBarsInsets.bottom)
            insets
        }

        rootView.requestApplyInsets()
    }
}

@Suppress("DEPRECATION")
fun Activity.startActivityWithTransition(intent: Intent) {

    val destinationClass = intent.component?.className ?: return

    try {

        val clazz = Class.forName(destinationClass)
        val transition = clazz.getAnnotation(TransitionEffects::class.java)

        startActivity(intent)

        if (transition != null) {

            val enter = getAnimationResource(transition.enter)
            val exit = getAnimationResource(transition.exit)

            overridePendingTransition(enter, exit)
        }

    } catch (e: Exception) {

        recordException(e)

        startActivity(intent)
    }
}

@Suppress("DEPRECATION")
fun Activity.finishWithTransition() {

    val transition = this::class.java.getAnnotation(TransitionEffects::class.java)

    finish()

    if (transition != null) {

        val enter = getAnimationResource(transition.enter)
        val exit = getAnimationResource(transition.exit)

        overridePendingTransition(enter, exit)
    }
}

@SuppressLint("DiscouragedApi")
fun Activity.getAnimationResource(animName: String): Int {

    val type = "anim"

    return resources.getIdentifier(animName, type, packageName)
}

fun DialogFragment.fitInsideSystemBoundaries() {

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {

        activity?.getSystemBarsInsets { top, bottom ->

            view?.setPadding(0, top, 0, bottom)
        }
    }
}

fun Activity.openLink(url: Int) {

    val url = getString(url)

    val tag = "Open link :: Resource = $url ::"

    Console.log("$tag Url = $url")

    openLink(url)
}

fun Activity.openLink(url: String) {

    val tag = "Open link ::"

    Console.log("$tag Url = $url")

    val uri = url.toUri()
    openUri(uri)
}

fun Activity.openUri(uri: Uri): Boolean {

    val tag = "Open Uri ::"

    Console.log("$tag Uri = $uri")

    val intent = Intent(Intent.ACTION_VIEW, uri)
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    try {

        startActivity(intent)

        return true

    } catch (_: ActivityNotFoundException) {

        Console.error("$tag Activity has not been found")
    }

    return false
}