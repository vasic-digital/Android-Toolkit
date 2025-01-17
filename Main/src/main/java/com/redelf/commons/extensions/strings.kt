package com.redelf.commons.extensions

import android.annotation.SuppressLint
import android.graphics.Color
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Base64
import androidx.core.content.ContextCompat
import com.redelf.commons.application.BaseApplication
import com.redelf.commons.logging.Console
import com.redelf.commons.security.obfuscation.DefaultObfuscator
import com.redelf.commons.security.obfuscation.Obfuscation
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorInputStream
import org.apache.commons.compress.compressors.lz4.BlockLZ4CompressorOutputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.lang.StringBuilder
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

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

fun String.toBase64(): String {

    try {

        val inputBytes = this.toByteArray()
        val encodedBytes = Base64.encode(inputBytes, Base64.DEFAULT)

        return encodedBytes.toString(Charsets.UTF_8)

    } catch (e: Exception) {

        recordException(e)
    }

    return ""
}

fun String.fromBase64(): String {

    if (this.isBase64Encoded()) {

        try {

            val encodedBytes = this.toByteArray(Charsets.UTF_8)
            val decodedBytes = Base64.decode(encodedBytes, Base64.DEFAULT)

            return String(decodedBytes)

        } catch (e: Exception) {

            recordException(e)
        }
    }

    return this
}

fun String.compress(lz4: Boolean = true): ByteArray? {

    val uncompressed = this

    if (isEmpty(uncompressed)) {

        return null
    }

    try {

        val byteOS = ByteArrayOutputStream()

        if (lz4) {

            val lz4Out = BlockLZ4CompressorOutputStream(byteOS)

            lz4Out.write(uncompressed.toByteArray())
            lz4Out.close()

            return byteOS.toByteArray()

        } else {

            val gzipOut = GZIPOutputStream(byteOS)

            gzipOut.write(uncompressed.toByteArray())
            gzipOut.close()

            return byteOS.toByteArray()
        }


    } catch (e: IOException) {

        Console.error(e)

        return null
    }
}

fun ByteArray.decompress(lz4: Boolean = true): String? {

    if (this.isEmpty()) {

        return null
    }

    try {

        if (lz4) {

            val lz4In = BlockLZ4CompressorInputStream(ByteArrayInputStream(this))

            val byteArrayOS = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int

            while (lz4In.read(buffer).also { bytesRead = it } != -1) {

                byteArrayOS.write(buffer, 0, bytesRead)
            }

            return String(byteArrayOS.toByteArray(), Charsets.UTF_8)

        } else {

            val gzipIn = GZIPInputStream(ByteArrayInputStream(this))

            val byteArrayOS = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var bytesRead: Int

            while (gzipIn.read(buffer).also { bytesRead = it } != -1) {

                byteArrayOS.write(buffer, 0, bytesRead)
            }

            return String(byteArrayOS.toByteArray(), Charsets.UTF_8)
        }

    } catch (e: IOException) {

        Console.error(e)

        return null
    }
}

@Throws(IOException::class)
fun String.compressAndEncrypt(

    secretKey: SecretKey? = BaseApplication.takeContext().getSecret()

): String {

    try {

        val compressed = compress(true)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, ByteArray(12)) // 12-byte IV for GCM

        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val encryptedData = cipher.doFinal(compressed)

        return java.util.Base64.getEncoder().encodeToString(encryptedData)

    } catch (e: Exception) {

        recordException(e)
    }

    return this
}

@Throws(IOException::class)
fun String.decryptAndDecompress(

    secretKey: SecretKey? = BaseApplication.takeContext().getSecret()

): String {

    try {

        val encryptedData = java.util.Base64.getDecoder().decode(this)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, ByteArray(12)) // 12-byte IV for GCM

        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)

        val decryptedData = cipher.doFinal(encryptedData)

        return decryptedData.decompress(true) ?: this

    } catch (e: Exception) {

        recordException(e)
    }

    return this
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

fun String.color(

    color: Int,
    words: List<String>,
    ss: SpannableString = SpannableString(this)

): SpannableString {

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

fun String.color(

    color: String,
    words: List<String>,
    ss: SpannableString = SpannableString(this)

): SpannableString {

    words.forEach { word ->

        try {

            val startIndex = indexOf(word)

            if (startIndex != -1 && isNotEmpty(color)) {

                val endIndex = startIndex + word.length
                val c = Color.parseColor(color)

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

fun String.forClassName(): String {

    return this.replace("\"", "").replace("\'", "").trim()
}

/*
*   TODO: Map to be dynamic for the obfuscation purposes
*/
private val digitToLetterMap = mapOf(

    0 to 'b', 1 to 'a', 2 to 'd', 3 to 'c', 4 to 'e',
    5 to 'j', 6 to 'g', 7 to 'i', 8 to 'h', 9 to 'f'
)

fun String.hashCodeString(): String {

    val b = StringBuilder()

    this.hashCode().toString().forEach { letter ->

        val digit = letter.code
        val letter = digitToLetterMap[digit]

        b.append(letter)
    }

    return b.toString()
}
