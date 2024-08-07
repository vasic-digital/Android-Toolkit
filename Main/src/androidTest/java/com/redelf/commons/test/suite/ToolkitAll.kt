package com.redelf.commons.test.suite

import com.redelf.commons.test.DataDelegatePartitioningTest
import com.redelf.commons.test.EncryptedPersistenceTest
import com.redelf.commons.test.HttpEndpointsTest
import com.redelf.commons.test.ObfuscatorTest
import org.junit.runner.RunWith
import org.junit.runners.Suite
import org.junit.runners.Suite.SuiteClasses

@SuiteClasses(

    DataDelegatePartitioningTest::class,
    EncryptedPersistenceTest::class,
    HttpEndpointsTest::class,
    ObfuscatorTest::class

)
@RunWith(Suite::class)
class ToolkitAll