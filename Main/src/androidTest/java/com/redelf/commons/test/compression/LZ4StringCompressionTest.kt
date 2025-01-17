package com.redelf.commons.test.compression

import com.redelf.commons.extensions.GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK
import com.redelf.commons.extensions.compress
import com.redelf.commons.extensions.compressAndEncrypt
import com.redelf.commons.extensions.decompress
import com.redelf.commons.extensions.decryptAndDecompress
import com.redelf.commons.extensions.recordException
import com.redelf.commons.logging.Console
import com.redelf.commons.test.BaseTest
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import javax.crypto.KeyGenerator

class LZ4StringCompressionTest : BaseTest() {

    @Before
    fun prepare() {

        Console.initialize(failOnError = true)

        Console.log("Console initialized: $this")

        GLOBAL_RECORD_EXCEPTIONS_ASSERT_FALLBACK.set(true)
    }

    @Test
    fun testLZ4() {

        val text =
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec euismod, nulla sit amet ultrices ultrices, ante massa tincidunt ante, eu tincidunt turpis ante eu ante. "

        val compressed = text.compress(lz4 = true)
        val decompressed = compressed?.decompress(lz4 = true)

        Assert.assertNotNull(compressed)
        Assert.assertNotEquals(text, compressed)
        Assert.assertEquals(text, decompressed)
        Assert.assertTrue(compressed?.isNotEmpty() == true)

        val textLength = text.length
        val compressedLength = compressed?.size ?: 0

        Assert.assertTrue(compressedLength > 0)
        Assert.assertTrue(compressedLength < textLength)
    }

    @Test
    fun testLZ4WithEncryption() {

        val tag = "Test :: LZ4WithEncryption ::"

        var text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Donec euismod, " +
                "nulla sit amet ultrices ultrices, ante massa tincidunt ante, eu " +
                "tincidunt turpis ante eu ante."

        Console.log("$tag START")

        /*
        * FIXME: java.lang.OutOfMemoryError: Failed to allocate a 311951376 byte allocation with 25165824 free bytes and 191MB until OOM, target footprint 361643744, growth limit 536870912
            at java.util.Arrays.copyOf(Arrays.java:3585)
            at java.io.ByteArrayOutputStream.grow(ByteArrayOutputStream.java:120)
            at java.io.ByteArrayOutputStream.ensureCapacity(ByteArrayOutputStream.java:95)
            at java.io.ByteArrayOutputStream.write(ByteArrayOutputStream.java:156)
            at com.redelf.commons.extensions.StringsKt.decompress(strings.kt:156)
            at com.redelf.commons.extensions.StringsKt.decryptAndDecompress(strings.kt:230)
            at com.redelf.commons.extensions.StringsKt.decryptAndDecompress$default(strings.kt:213)
            at com.redelf.commons.test.compression.LZ4StringCompressionTest.testLZ4WithEncryption(LZ4StringCompressionTest.kt:82)
            at java.lang.reflect.Method.invoke(Native Method)
            at org.junit.runners.model.FrameworkMethod$1.runReflectiveCall(FrameworkMethod.java:59)
            at org.junit.internal.runners.model.ReflectiveCallable.run(ReflectiveCallable.java:12)
            at org.junit.runners.model.FrameworkMethod.invokeExplosively(FrameworkMethod.java:56)
            at org.junit.internal.runners.statements.InvokeMethod.evaluate(InvokeMethod.java:17)
            at androidx.test.internal.runner.junit4.statement.RunBefores.evaluate(RunBefores.java:80)
            at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
            at org.junit.runners.BlockJUnit4ClassRunner$1.evaluate(BlockJUnit4ClassRunner.java:100)
            at org.junit.runners.ParentRunner.runLeaf(ParentRunner.java:366)
            at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:103)
            at org.junit.runners.BlockJUnit4ClassRunner.runChild(BlockJUnit4ClassRunner.java:63)
            at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
            at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
            at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
            at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
            at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
            at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
            at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
            at org.junit.runners.Suite.runChild(Suite.java:128)
            at org.junit.runners.Suite.runChild(Suite.java:27)
            at org.junit.runners.ParentRunner$4.run(ParentRunner.java:331)
            at org.junit.runners.ParentRunner$1.schedule(ParentRunner.java:79)
            at org.junit.runners.ParentRunner.runChildren(ParentRunner.java:329)
            at org.junit.runners.ParentRunner.access$100(ParentRunner.java:66)
            at org.junit.runners.ParentRunner$2.evaluate(ParentRunner.java:293)
            at org.junit.runners.ParentRunner$3.evaluate(ParentRunner.java:306)
            at org.junit.runners.ParentRunner.run(ParentRunner.java:413)
            at org.junit.runner.JUnitCore.run(JUnitCore.java:137)
            at org.junit.runner.JUnitCore.run(JUnitCore.java:115)
            at androidx.test.internal.runner.TestExecutor.execute(TestExecutor.java:68)
            at androidx.test.internal.runner.TestExecutor.execute(TestExecutor.java:59)
            at androidx.test.runner.AndroidJUnitRunner.onStart(AndroidJUnitRunner.java:463)
            at android.app.Instrumentation$InstrumentationThread.run(Instrumentation.java:2415)
        */
        (0..100).forEach {

            text += text

            val tag = "$tag Iteration = ${it + 1} ::"

            var start = System.currentTimeMillis()

            val compressed = text.compressAndEncrypt()

            Console.log(

                "$tag COMPRESSED :: Time = ${System.currentTimeMillis() - start} ms, " +
                        "Size = ${compressed.toByteArray().size} bytes, " +
                        "Original Size = ${text.toByteArray().size} bytes"
            )

            Assert.assertNotNull(compressed)
            Assert.assertNotEquals(text, compressed)

            start = System.currentTimeMillis()

            val decompressed = compressed.decryptAndDecompress()

            Console.log(

                "$tag DECOMPRESSED :: Time = ${System.currentTimeMillis() - start} ms, " +
                        "Size = ${decompressed.toByteArray().size} bytes, " +
                        "Original Size = ${text.toByteArray().size} bytes"
            )

            Assert.assertEquals(text, decompressed)
            Assert.assertTrue(compressed.isNotEmpty())
        }

        Console.log("$tag END")
    }
}