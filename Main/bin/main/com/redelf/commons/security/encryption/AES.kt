package com.redelf.commons.security.encryption

import android.os.Build
import android.util.Base64
import com.redelf.commons.extensions.isBase64Encoded
import com.redelf.commons.logging.Console
import java.security.GeneralSecurityException
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec


class AES(private val key: String, private val salt: String) : Encryption<String, String> {

    private val keyLength = 256
    private val keyAlgorithm = "AES"
    private val iterationCount = 6553
    private val transformation = "AES/CBC/PKCS5Padding"
    private val charset = charset("UTF-8")

    private val algorithm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        "PBKDF2WithHmacSHA256"
    } else {
        "PBKDF2withHmacSHA1And8BIT"
    }

    @Throws(GeneralSecurityException::class)
    override fun encrypt(data: String): String {

        Console.log("AES: encrypt()")

        val (ivSpec, secretKey, cipher) = getCipher()
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec)
        val ciphered = cipher.doFinal(data.toByteArray(charset))
        return Base64.encodeToString(ciphered, Base64.DEFAULT)
    }

    @Throws(GeneralSecurityException::class, IllegalArgumentException::class)
    override fun decrypt(source: String): String {

        Console.log("AES: decrypt()")

        val (ivSpec, secretKey, cipher) = getCipher()
        cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

        val decoded = if (source.isBase64Encoded()) {

            Base64.decode(source, Base64.DEFAULT)

        } else {

            source.toByteArray()
        }

        val ciphered = cipher.doFinal(decoded)
        return String(ciphered)
    }

    @Throws(GeneralSecurityException::class)
    private fun getCipher(): Triple<IvParameterSpec, SecretKeySpec, Cipher> {

        val iv = emptyBytes()
        val ivSpec = IvParameterSpec(iv)
        val alg = algorithm
        Console.info("Algorithm: $alg")
        val factory = SecretKeyFactory.getInstance(alg)
        val chars = key.toCharArray()
        val bytes = salt.toByteArray()
        val spec: KeySpec = PBEKeySpec(chars, bytes, iterationCount, keyLength)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, keyAlgorithm)
        val cipher = Cipher.getInstance(transformation)
        return Triple(ivSpec, secretKey, cipher)
    }

    private fun emptyBytes() = byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
}