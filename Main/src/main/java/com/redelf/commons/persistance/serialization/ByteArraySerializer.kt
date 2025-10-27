/*
 * Copyright (c) 2025 MeTube Share
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.redelf.commons.persistance.serialization

import android.content.Context
import android.util.Base64
import com.redelf.commons.extensions.hashCodeString
import com.redelf.commons.extensions.recordException
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Salter
import com.redelf.commons.persistance.encryption.CompressedEncryption
import com.redelf.commons.persistance.encryption.NoEncryption
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

/*
* TODO:
*   - Support compression
*   - Support data removal
*/
class ByteArraySerializer(

    ctx: Context,
    key: String,

    private var encrypt: Boolean,
    private var encryption: Encryption<String>? = null,

    salter: Salter = object : Salter {

        override fun getSalt() = key.hashCodeString().reversed()
    }

) : Serializer {

    private val sPrefs = ctx.getSharedPreferences(key, Context.MODE_PRIVATE)

    init {

        if (encryption == null) {

            encryption = instantiateDefaultEncryption(ctx, salter)
        }
    }

    override fun takeClass(): Class<*> {

        return ByteArray::class.java
    }

    override fun serialize(key: String, value: Any): Boolean {

        try {

            val editor = sPrefs.edit()

            val byteArrayOutputStream = ByteArrayOutputStream()
            val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
            objectOutputStream.writeObject(value)

            var byteArrayValue = byteArrayOutputStream.toByteArray()
            var encoded = Base64.encodeToString(byteArrayValue, Base64.DEFAULT)
            val encrypted = encryption?.encrypt(key, encoded)

            if (encrypted == null) {

                throw IllegalArgumentException("Encryption failed")
            }

            encoded = Base64.encodeToString(encrypted.toByteArray(), Base64.DEFAULT)

            editor.putString(key, encoded)
            val result = editor.commit()

            objectOutputStream.close()
            byteArrayOutputStream.close()

            return result

        } catch (e: Throwable) {

            recordException(e)
        }

        return false
    }

    override fun deserialize(key: String): ByteArray? {

        try {

            val encoded = sPrefs.getString(key, null)

            if (encoded == null) {

                return null
            }

            var decodedValue = Base64.decode(encoded, Base64.DEFAULT)

            val decrypted = encryption?.decrypt(key, String(decodedValue))

            if (decrypted == null) {

                throw IllegalArgumentException("Decryption failed")
            }

            decodedValue = Base64.decode(decrypted, Base64.DEFAULT)

            val byteArrayInputStream = ByteArrayInputStream(decodedValue)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)

            val result = objectInputStream.readObject() as ByteArray

            objectInputStream.close()
            byteArrayInputStream.close()

            return result

        } catch (e: Throwable) {

            recordException(e)
        }

        return null
    }

    private fun instantiateDefaultEncryption(context: Context, salter: Salter): Encryption<String> {

        if (encrypt) {

            return CompressedEncryption()
        }

        return NoEncryption()
    }
}