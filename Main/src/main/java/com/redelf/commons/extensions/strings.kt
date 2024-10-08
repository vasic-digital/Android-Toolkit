package com.redelf.commons.extensions

import android.annotation.SuppressLint
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Base64
import androidx.core.content.ContextCompat
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Console
import com.redelf.commons.security.obfuscation.DefaultObfuscator
import com.redelf.commons.security.obfuscation.Obfuscation
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

fun String.deobfuscate(deobfuscator: Obfuscation = DefaultObfuscator): String {

    try {

        return deobfuscator.deobfuscate(this)

    } catch (e: Exception) {

        recordException(e)
    }

    return ""
}

fun String.obfuscate(obfuscator: Obfuscation = DefaultObfuscator): String {

    try {

        return obfuscator.obfuscate(this)

    } catch (e: Exception) {

        recordException(e)
    }

    return ""
}

fun String.isBase64Encoded(): Boolean {

    return org.apache.commons.codec.binary.Base64.isBase64(this)
}

fun String.compress(): String? {

    val uncompressed = this

    if (isEmpty(uncompressed)) {

        return null
    }

    try {

        val byteOS = ByteArrayOutputStream()
        val gzipOut = GZIPOutputStream(byteOS)

        gzipOut.write(uncompressed.toByteArray())
        gzipOut.close()

        return Base64.encodeToString(byteOS.toByteArray(), Base64.DEFAULT)

    } catch (e: IOException) {

        Console.error(e)

        return null
    }
}

fun String.decompress(): String? {

    val compressed: String = this

    if (isEmpty(compressed)) {

        return null
    }

    try {

        val compressedData = Base64.decode(compressed, Base64.DEFAULT)
        val byteArrayIS = ByteArrayInputStream(compressedData)
        val gzipIn = GZIPInputStream(byteArrayIS)
        val byteArrayOS = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var bytesRead: Int

        while (gzipIn.read(buffer).also { bytesRead = it } != -1) {

            byteArrayOS.write(buffer, 0, bytesRead)
        }

        return String(byteArrayOS.toByteArray(), Charsets.UTF_8)

    } catch (e: IOException) {

        Console.error(e)

        return null
    }
}

fun String.snakeCase(): String {

    val regex = Regex("([a-z])([A-Z])")

    val result = regex.replace(this) {

            matchResult ->
        matchResult.groupValues[1] + "_" + matchResult.groupValues[2]
    }

    return result.lowercase()
}

fun String.toResourceName() = this.snakeCase()

@SuppressLint("DiscouragedApi")
fun String.toResource(type: String, fallback: Int = 0): Int {

    val tag = "String.toResource :: $type ::"

    if (isEmpty(this)) {

        Console.error("$tag Empty :: Key is empty")

        return fallback
    }

    val snakeCase = this.toResourceName()
    val logKey = "Key = $this ( $snakeCase )"

    try {

        val ctx = BaseApplication.takeContext()
        val res = ctx.resources

        val resId = res.getIdentifier(snakeCase, type, ctx.packageName)

        if (resId > 0) {

            return resId

        } else {

            Console.error("$tag Not found :: $logKey")
        }

    } catch (e: Exception) {

        Console.error("$tag Failed :: $logKey, Error = ${e.message}")

        recordException(e)
    }

    return fallback
}

fun String.toDrawableResource() = this.toResource("drawable")

fun String.toColorResource() = this.toResource("color")

fun String.toDimenResource() = this.toResource("dimen")

fun String.toFontResource() = this.toResource("font")

fun String.toStringResource() = this.toResource("string")

fun String.toStyleResource() = this.toResource("style")

fun String.toXmlResource() = this.toResource("xml")

fun String.localized(fallback: String = ""): String {

    val tag = "String.localized ::"

    if (isEmpty(this)) {

        Console.error("$tag Empty :: Key is empty")

        return fallback
    }

    try {

        val res = this.toStringResource()
        val ctx = BaseApplication.takeContext()

        if (res > 0) {

            val str = ctx.getString(res)

            if (isEmpty(str)) {

                Console.warning("$tag Empty :: Key = $this")

                return fallback
            }

            return str

        } else {

            Console.error("$tag Not found :: Key = $this")
        }

    } catch (e: Exception) {

        Console.error("$tag Failed :: Key = $this, Error = ${e.message}")

        recordException(e)
    }

    return fallback
}

fun String.format(vararg args: Any): String {

    return string(this.localized(), *args)
}

fun string(format: String, vararg args: Any): String {

    var value = format.localized()

    args.forEach {

        var oldValue = ""

        if (it is Number) {

            oldValue = "%d"
        }

        if (it is String) {

            oldValue = "%s"
        }

        if (it is Boolean) {

            oldValue = "%b"
        }

        if (it is Char) {

            oldValue = "%c"
        }

        if (isNotEmpty(oldValue)) {

            value = value.replaceFirst(

                oldValue = oldValue,
                newValue = it.toString(),
                ignoreCase = true
            )
        }
    }

    return value
}

fun String.color(color: Int, vararg words: String): SpannableString {

    val ss = SpannableString(this)

    words.forEach { word ->

        try {

            val startIndex = indexOf(word)

            if (startIndex != -1) {

                val endIndex = startIndex + word.length
                val ctx = BaseApplication.takeContext()
                val c = ContextCompat.getColor(ctx, color)

                ss.setSpan(

                    ForegroundColorSpan(c),
                    startIndex,
                    endIndex,
                    0
                )

                return ss
            }

        } catch (e: Exception) {

            recordException(e)
        }
    }

    return ss
}
