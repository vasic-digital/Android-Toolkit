package com.redelf.commons.test.suite

import com.redelf.commons.test.DataDelegatePartitioningTest
import com.redelf.commons.test.DataManagementTest
import com.redelf.commons.test.EncryptedPersistenceTest
import com.redelf.commons.test.ExecutorTest
import com.redelf.commons.test.GsonParserTest
import com.redelf.commons.test.HttpEndpointsTest
import com.redelf.commons.test.ObfuscatorTest
import com.redelf.commons.test.compression.LZ4StringCompressionTest
import com.redelf.commons.test.serialization.ByteArraySerializerTest
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

@SuiteClasses(

    DataDelegatePartitioningTest::class,
    EncryptedPersistenceTest::class,
    HttpEndpointsTest::class,
    ObfuscatorTest::class,
    LZ4StringCompressionTest::class,
    ByteArraySerializerTest::class,
    GsonParserTest::class,
    DataManagementTest::class,
    ExecutorTest::class

)
@RunWith(Suite::class)
class ToolkitAll