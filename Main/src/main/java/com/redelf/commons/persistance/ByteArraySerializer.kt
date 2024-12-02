package com.redelf.commons.persistance

import android.content.Context
import android.util.Base64
import com.redelf.commons.extensions.recordException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream

class ByteArraySerializer(ctx: Context, key: String) {

    private val sPrefs = ctx.getSharedPreferences(key, Context.MODE_PRIVATE)

    fun serialize(key: String, byteArray: ByteArray): Boolean {

        try {

            val editor = sPrefs.edit()

            val byteArrayOutputStream = ByteArrayOutputStream()
            val objectOutputStream = ObjectOutputStream(byteArrayOutputStream)
            objectOutputStream.writeObject(byteArray)

            val byteArrayValue = byteArrayOutputStream.toByteArray()
            val encoded = Base64.encodeToString(byteArrayValue, Base64.DEFAULT)

            editor.putString(key, encoded)
            val result = editor.commit()

            objectOutputStream.close()
            byteArrayOutputStream.close()

            return result

        } catch (e: Exception) {

            recordException(e)
        }

        return false
    }

    fun deserialize(key: String): ByteArray? {

        try {

            val encoded = sPrefs.getString(key, null)

            if (encoded == null) {

                return null
            }

            val decodedValue = Base64.decode(encoded, Base64.DEFAULT)

            val byteArrayInputStream = ByteArrayInputStream(decodedValue)
            val objectInputStream = ObjectInputStream(byteArrayInputStream)

            val result = objectInputStream.readObject() as ByteArray

            objectInputStream.close()
            byteArrayInputStream.close()

            return result

        } catch (e: Exception) {

            recordException(e)
        }

        return null
    }
}