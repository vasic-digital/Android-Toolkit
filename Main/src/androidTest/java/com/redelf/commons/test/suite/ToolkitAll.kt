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


package com.redelf.commons.test.suite

import com.redelf.commons.test.DataDelegatePartitioningTest
import com.redelf.commons.test.DataManagementTest
import com.redelf.commons.test.EncryptedPersistenceTest
import com.redelf.commons.test.ExecutorTest
import com.redelf.commons.test.GsonParserTest
import com.redelf.commons.test.HttpEndpointsTest
import com.redelf.commons.test.ListWrapperTest
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
    ExecutorTest::class,
    ListWrapperTest::class

)
@RunWith(Suite::class)
class ToolkitAll