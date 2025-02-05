package com.redelf.commons.persistance.serialization

import android.content.Context
import android.util.Base64
import com.redelf.commons.extensions.hashCodeString
import com.redelf.commons.extensions.recordException
import com.redelf.commons.persistance.CompressedEncryption
import com.redelf.commons.persistance.ConcealEncryption
import com.redelf.commons.persistance.NoEncryption
import com.redelf.commons.persistance.base.Encryption
import com.redelf.commons.persistance.base.Salter
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

        if (encryption is ConcealEncryption) {

            encryption?.init()
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

        } catch (e: Exception) {

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

        } catch (e: Exception) {

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