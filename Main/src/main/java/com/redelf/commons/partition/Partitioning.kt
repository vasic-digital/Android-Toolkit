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


package com.redelf.commons.partition

import com.redelf.commons.data.type.Typed
import java.lang.reflect.Type

/*
* TODO: Make sure that is possible to write out only the differences (changes)
*   - Hint: CopyOnWriteArrayList
*   - Hint: How Docker images work
*/
interface Partitioning<T> : Typed<T> {

    fun isPartitioningEnabled(): Boolean

    fun isPartitioningParallelized(): Boolean

    fun getPartitionCount(): Int

    fun getPartitionData(number: Int): Any?

    fun isPartitionCollection(number: Int): Boolean? = null

    /*
        TODO: To be fully-automatic, with possibility of override and automatic data conversion
    */
    fun setPartitionData(number: Int, data: Any?): Boolean

    fun failPartitionData(number: Int, error: Throwable)

    /*
        TODO: To be fully-automatic, with possibility of override
    */
    fun getPartitionType(number: Int): Type?
}