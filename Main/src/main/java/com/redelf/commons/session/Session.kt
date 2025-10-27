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


package com.redelf.commons.session

import com.redelf.commons.destruction.reset.Resettable
import com.redelf.commons.execution.ExecuteWithResult
import com.redelf.commons.logging.Console
import java.util.UUID

class Session(

    private var identifier: UUID = UUID.randomUUID(),
    private var name: String = identifier.toString()

) :

    Resettable, ExecuteWithResult<SessionOperation>

{

    init {

        Console.debug("Created :: Session: $identifier @ $name")
    }

    fun takeName() = name

    fun takeIdentifier() = identifier

    override fun execute(what: SessionOperation): Boolean {

        val transactionId = identifier

        Console.log("$name :: Execute :: START :: Session: $transactionId")

        val started = what.start()

        if (started) {

            Console.log("$name :: Execute :: STARTED :: Session: $transactionId")

            val success = what.perform()

            if (success) {

                Console.log(

                    "$name :: Execute :: PERFORMED :: Session: $transactionId :: Success"
                )

            } else {

                Console.error(

                    "$name :: Execute :: PERFORMED :: Session: $transactionId :: Failure"
                )
            }

            if (success && transactionId == identifier) {

                Console.log("$name :: Execute :: ENDING :: Session: $transactionId")

                val ended = what.end(true)

                if (ended) {

                    Console.log(

                        "$name :: Execute :: ENDED :: Session: $transactionId :: Success"
                    )

                } else {

                    Console.log(

                        "$name :: Execute :: ENDED :: Session: $transactionId :: Failure"
                    )
                }

                return ended

            } else {

                if (transactionId != identifier) {

                    Console.warning("$name :: Execute :: ENDED :: Session: Skipped")
                }
            }
        }

        return false
    }

    override fun reset(): Boolean {

        val oldId = identifier
        val oldName = name

        identifier = UUID.randomUUID()

        if (name == oldId.toString()) {

            name = identifier.toString()
        }

        Console.debug("$oldName :: Reset :: Session: $oldId -> $identifier")

        return oldId != identifier
    }
}