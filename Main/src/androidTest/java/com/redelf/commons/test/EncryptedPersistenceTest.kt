package com.redelf.commons.test

import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.logging.Console
import com.redelf.commons.persistance.DBStorage
import com.redelf.commons.persistance.EncryptedPersistence
import com.redelf.commons.security.encryption.EncryptionListener
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class EncryptedPersistenceTest : BaseTest() {

    private lateinit var persistence: EncryptedPersistence

    @Before
    fun prepare() {

        Console.initialize(failOnError = true)

        Console.log("Console initialized: $this")

        GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK.set(true)

        val testTag = "test.${System.currentTimeMillis()}"

        DBStorage.initialize(ctx = applicationContext)

        try {

            persistence = EncryptedPersistence(

                doLog = true,
                storageTag = testTag,
                ctx = applicationContext,
            )

            Assert.assertTrue(persistence.isEncryptionEnabled())

        } catch (e: Exception) {

            Assert.fail(e.message)
        }
    }

    @Test
    fun testBoolean() {

        log("Boolean testing: START")

        val booleanValueKey = "testBool"
        var persistOK = persistence.push(booleanValueKey, true)

        Assert.assertTrue(persistOK)

        var retrievedBooleanValue = persistence.pull<Boolean>(booleanValueKey)

        Assert.assertNotNull(retrievedBooleanValue)
        Assert.assertTrue(retrievedBooleanValue == true)

        persistOK = persistence.push(booleanValueKey, false)

        Assert.assertTrue(persistOK)

        Assert.assertNotNull(retrievedBooleanValue)
        retrievedBooleanValue = persistence.pull(booleanValueKey)

        Assert.assertFalse(retrievedBooleanValue ?: true)

        log("Boolean testing: END")
    }

    @Test
    fun testNumbers() {

        log("Numbers testing: START")

        val numbers = listOf(1, 2, 21, 1.0, 0.1, 1.0000000001, 100, 1000, 100000)

        numbers.forEach { number ->

            testNumber(number)
        }

        log("Numbers testing: END")
    }

    @Test
    fun testRandomPositiveNumbers() {

        log("Positive numbers testing: START")

        val count = 10

        (0..count).forEach { x ->

            val number = (1..Int.MAX_VALUE).random()
            testNumber(number)
        }

        (0..count).forEach { x ->

            val number = Random.nextDouble(0.001, Double.MAX_VALUE)
            testNumber(number)
        }

        log("Positive numbers testing: END")
    }

    @Test
    fun testRandomNegativeNumbers() {

        log("Negative numbers testing: START")

        val count = 10

        (0..count).forEach { x ->

            val number = (Int.MIN_VALUE..-1).random()
            testNumber(number)
        }

        (0..count).forEach { x ->

            val number = Random.nextDouble(-999999999.999, -0.001)
            testNumber(number)
        }


        log("Negative numbers testing: END")
    }

    @Test
    fun testString() {

        log("String testing: START")

        val strings = listOf(
            "Hello", "World", "I need the floating point values to at " +
                    "least 4 decimals, preferably 7"
        )

        val stringValueKey = "testString"

        strings.forEach {

            val persistOK = persistence.push(stringValueKey, it)

            Assert.assertTrue(persistOK)

            val retrievedStringValue = persistence.pull<String>(stringValueKey)
            Assert.assertEquals(it, retrievedStringValue)
        }

        log("String testing: END")
    }

    @Test
    fun testUtfString() {

        log("UTF string testing: START")

        val strings = listOf(

            "Шш Ђђ Чч Ћћ Љљ"
        )

        val stringValueKey = "testUtfString"

        strings.forEach {

            val persistOK = persistence.push(stringValueKey, it)

            Assert.assertTrue(persistOK)

            val retrievedStringValue = persistence.pull<String>(stringValueKey)
            Assert.assertEquals(it, retrievedStringValue)
        }

        log("UTF string testing: END")
    }

    private fun testNumber(number: Number) {

        val latch = CountDownLatch(2)

        var encKey = ""
        var encRaw = ""
        var enc = ""

        var decKey = ""
        var decEnc = ""
        var dec = ""

        val callback = object : EncryptionListener<String, String> {

            override fun onEncrypted(

                key: String,
                raw: String,
                encrypted: String

            ) {

                Console.log("On :: Encrypted :: Key = $key, Raw = $raw, Encrypted = $encrypted")

                encKey = key
                encRaw = raw
                enc = encrypted

                latch.countDown()
            }

            override fun onDecrypted(

                key: String,
                encrypted: String,
                decrypted: String

            ) {

                Console.log(

                    "On :: Encrypted :: Key = $key, Encrypted = $encrypted, Decrypted = $decrypted"
                )

                decKey = key
                decEnc = encrypted
                dec = decrypted

                latch.countDown()
            }

            override fun onEncryptionFailure(key: String, error: Throwable) {

                Assert.fail("ASSERT FAILURE :: Key = $key, Error = ${error.message}")
            }

            override fun onDecryptionFailure(key: String, error: Throwable) {

                Assert.fail("ASSERT FAILURE :: Key = $key, Error = ${error.message}")
            }
        }

        persistence.register(callback)

        val numberValueKey = "testNumber"

        val persistOK = persistence.push(numberValueKey, number)

        Assert.assertTrue(persistOK)

        val retrieved = persistence.pull<Any?>(numberValueKey)

        try {

            if (!latch.await(15, TimeUnit.SECONDS)) {

                Assert.fail("Latch timed out")
            }

        } catch (e: Exception) {

            Assert.fail(e.message)
        }

        Assert.assertTrue(encKey.isNotEmpty())
        Assert.assertTrue(encRaw.isNotEmpty())
        Assert.assertTrue(enc.isNotEmpty())

        Assert.assertTrue(decKey.isNotEmpty())
        Assert.assertTrue(decEnc.isNotEmpty())
        Assert.assertTrue(dec.isNotEmpty())

        Assert.assertEquals(numberValueKey, encKey)
        Assert.assertEquals(numberValueKey, decKey)
        Assert.assertEquals(encKey, decKey)

        Assert.assertNotEquals(encRaw, enc)
        Assert.assertNotEquals(decEnc, dec)

        Assert.assertEquals(number.toString(), encRaw)
        Assert.assertEquals(number.toString(), dec)

        Assert.assertNotNull(retrieved)
        Assert.assertTrue(retrieved is Number)
        Assert.assertTrue(number == retrieved)

        persistence.unregister(callback)
    }
}